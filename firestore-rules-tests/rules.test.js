// Checks app/firestore.rules against the Firestore emulator.
// Run with `npm test` in this folder (it starts and stops the emulator).
const fs = require("fs");
const path = require("path");
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require("@firebase/rules-unit-testing");
const { doc, getDoc, setDoc, updateDoc, deleteDoc, serverTimestamp, setLogLevel } = require("firebase/firestore");

// Rejected writes are expected; keep them out of the output.
setLogLevel("silent");

const RULES = fs.readFileSync(path.join(__dirname, "..", "app", "firestore.rules"), "utf8");

let env;
const kavindu = () => env.authenticatedContext("kavindu").firestore();
const other = () => env.authenticatedContext("someone-else").firestore();
const signedOut = () => env.unauthenticatedContext().firestore();

// A valid document for each collection; tests change one field at a time.
const entry = (over = {}) => ({
  status: "CONFIRMED",
  type: "EXPENSE",
  amount: 850,
  bonus: 0,
  category: "Coffee",
  account: "Main bank",
  date: "2026-11-05",
  note: "",
  spendingType: "DISCRETIONARY",
  usdCents: 0,
  effectiveRate: 0,
  feeLkr: 0,
  createdAt: serverTimestamp(),
  ...over,
});
const account = (over = {}) => ({
  name: "Main bank",
  description: "Salary account",
  kind: "BANK",
  createdAt: serverTimestamp(),
  ...over,
});
const goal = { name: "MacBook", targetAmount: 490000, savedAmount: 11200, targetDate: "2027-11-01" };
const plan = (over = {}) => ({
  payDay: 25,
  baseSalary: 120000,
  incomeSources: ["SALARY", "FREELANCE"],
  goal,
  updatedAt: serverTimestamp(),
  ...over,
});
const project = (over = {}) => ({
  name: "Bakery website",
  client: "Sweet Crumbs",
  totalBudget: 120000,
  milestones: [],
  updatedAt: serverTimestamp(),
  ...over,
});
const bill = (over = {}) => ({
  name: "Netflix",
  amount: 1990,
  account: "Main bank",
  category: "Subscriptions",
  dueDay: 5,
  isActive: true,
  lastCreatedMonth: null,
  updatedAt: serverTimestamp(),
  ...over,
});
const close = (over = {}) => ({
  month: "2026-10",
  balances: { "main-bank": 54000 },
  closedOn: "2026-11-01",
  ...over,
});

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-moneytrail",
    firestore: { rules: RULES },
  });
});
afterEach(() => env.clearFirestore());
after(() => env.cleanup());

describe("ownership", () => {
  it("lets a user read their own data", async () => {
    await assertSucceeds(getDoc(doc(kavindu(), "users/kavindu/entries/e1")));
  });
  it("blocks another user", async () => {
    await assertFails(getDoc(doc(other(), "users/kavindu/entries/e1")));
    await assertFails(setDoc(doc(other(), "users/kavindu/entries/e1"), entry()));
  });
  it("blocks a signed-out user", async () => {
    await assertFails(getDoc(doc(signedOut(), "users/kavindu/settings/plan")));
  });
});

describe("profile", () => {
  const profile = { name: "Kavindu", email: "k@example.com", createdAt: serverTimestamp() };
  it("accepts name, email and the server time", async () => {
    await assertSucceeds(setDoc(doc(kavindu(), "users/kavindu"), profile));
  });
  it("rejects extra fields", async () => {
    await assertFails(setDoc(doc(kavindu(), "users/kavindu"), { ...profile, admin: true }));
  });
});

describe("entries", () => {
  const ref = () => doc(kavindu(), "users/kavindu/entries/e1");
  it("accepts a valid expense", async () => {
    await assertSucceeds(setDoc(ref(), entry()));
  });
  it("accepts a salary with a bonus", async () => {
    await assertSucceeds(setDoc(ref(), entry({ type: "INCOME", category: "Salary", amount: 135000, bonus: 15000, spendingType: null })));
  });
  it("rejects a bonus on anything but salary", async () => {
    await assertFails(setDoc(ref(), entry({ type: "INCOME", category: "Freelance", bonus: 100, spendingType: null })));
  });
  it("rejects zero, negative and fractional amounts", async () => {
    await assertFails(setDoc(ref(), entry({ amount: 0 })));
    await assertFails(setDoc(ref(), entry({ amount: -5 })));
    await assertFails(setDoc(ref(), entry({ amount: 10.5 })));
  });
  it("rejects a bad date or status", async () => {
    await assertFails(setDoc(ref(), entry({ date: "05/11/2026" })));
    await assertFails(setDoc(ref(), entry({ status: "MAYBE" })));
  });
  it("needs a spending type on expenses and none on income", async () => {
    await assertFails(setDoc(ref(), entry({ spendingType: null })));
    await assertFails(setDoc(ref(), entry({ type: "INCOME", category: "Freelance" })));
  });
  it("only allows USD on income", async () => {
    await assertSucceeds(setDoc(ref(), entry({ type: "INCOME", category: "AdSense", spendingType: null, amount: 60000, usdCents: 20000, effectiveRate: 30000, feeLkr: 500 })));
    await assertFails(setDoc(ref(), entry({ usdCents: 20000, effectiveRate: 30000 })));
  });
  it("rejects a client-chosen createdAt", async () => {
    await assertFails(setDoc(ref(), entry({ createdAt: new Date("2020-01-01") })));
  });
  it("keeps createdAt when an entry is edited", async () => {
    await setDoc(ref(), entry());
    await assertSucceeds(updateDoc(ref(), { amount: 900 }));
    await assertFails(updateDoc(ref(), { createdAt: serverTimestamp() }));
  });
  it("lets the owner delete", async () => {
    await setDoc(ref(), entry());
    await assertSucceeds(deleteDoc(ref()));
  });
});

describe("accounts", () => {
  const ref = (id = "main-bank") => doc(kavindu(), `users/kavindu/accounts/${id}`);
  it("accepts a valid account and a balance check", async () => {
    await assertSucceeds(setDoc(ref(), account()));
    await assertSucceeds(updateDoc(ref(), { checkedBalance: 54000, checkedOn: "2026-11-05" }));
  });
  it("rejects an unknown kind", async () => {
    await assertFails(setDoc(ref(), account({ kind: "CREDIT_CARD" })));
  });
  it("rejects a fractional balance", async () => {
    await assertFails(setDoc(ref(), account({ checkedBalance: 10.5, checkedOn: "2026-11-05" })));
  });
});

describe("plan", () => {
  const ref = () => doc(kavindu(), "users/kavindu/settings/plan");
  it("accepts a valid plan", async () => {
    await assertSucceeds(setDoc(ref(), plan()));
  });
  it("rejects a pay day outside 1-31", async () => {
    await assertFails(setDoc(ref(), plan({ payDay: 32 })));
  });
  it("rejects savings above the goal", async () => {
    await assertFails(setDoc(ref(), plan({ goal: { ...goal, savedAmount: 500000 } })));
  });
  it("rejects an unknown income source", async () => {
    await assertFails(setDoc(ref(), plan({ incomeSources: ["LOTTERY"] })));
  });
});

describe("projects", () => {
  const ref = () => doc(kavindu(), "users/kavindu/projects/p1");
  it("accepts a valid project", async () => {
    await assertSucceeds(setDoc(ref(), project()));
  });
  it("rejects a project without a name", async () => {
    await assertFails(setDoc(ref(), project({ name: "" })));
  });
  it("rejects a price over the limit", async () => {
    await assertFails(setDoc(ref(), project({ totalBudget: 1000000000 })));
  });
});

describe("recurring bills", () => {
  const ref = () => doc(kavindu(), "users/kavindu/recurringBills/b1");
  it("accepts a valid bill, then one already put on the calendar", async () => {
    await assertSucceeds(setDoc(ref(), bill()));
    await assertSucceeds(setDoc(ref(), bill({ lastCreatedMonth: "2026-11" })));
  });
  it("rejects a due day outside 1-31", async () => {
    await assertFails(setDoc(ref(), bill({ dueDay: 0 })));
  });
  it("rejects a bad month", async () => {
    await assertFails(setDoc(ref(), bill({ lastCreatedMonth: "November" })));
  });
});

describe("categories", () => {
  const ref = () => doc(kavindu(), "users/kavindu/categories/c1");
  const cat = { name: "Pet care", type: "EXPENSE", spendingType: "COMMITTED", createdAt: serverTimestamp() };
  it("can be created and deleted but not changed", async () => {
    await assertSucceeds(setDoc(ref(), cat));
    await assertFails(updateDoc(ref(), { name: "Pets" }));
    await assertSucceeds(deleteDoc(ref()));
  });
  it("rejects a spending type on income", async () => {
    await assertFails(setDoc(ref(), { ...cat, type: "INCOME" }));
  });
});

describe("monthly closes", () => {
  it("accepts a close whose id is its month", async () => {
    await assertSucceeds(setDoc(doc(kavindu(), "users/kavindu/closes/2026-10"), close()));
  });
  it("rejects a close saved under another month", async () => {
    await assertFails(setDoc(doc(kavindu(), "users/kavindu/closes/2026-09"), close()));
  });
});

describe("everything else", () => {
  it("is private to its owner", async () => {
    await assertSucceeds(setDoc(doc(kavindu(), "users/kavindu/notes/n1"), { text: "anything" }));
    await assertFails(getDoc(doc(other(), "users/kavindu/notes/n1")));
  });
  it("can't be used to skip the checks above", async () => {
    await assertFails(setDoc(doc(kavindu(), "users/kavindu/entries/e1/extra/x"), { anything: true }));
  });
  it("blocks top-level collections", async () => {
    await assertFails(setDoc(doc(kavindu(), "config/anything"), { a: 1 }));
  });
});
