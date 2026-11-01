import { test, expect, Page } from "@playwright/test";
import { writeFileSync } from "node:fs";
const password = process.env.DEMO_PASSWORD || "Integration-Test-2026";
async function login(page: Page, role: string) {
  await page.goto("/");
  await page.getByLabel("Demo role").selectOption(role);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Enter workspace" }).click();
  await expect(
    page.getByRole("heading", { name: "The cage, at a glance." }),
  ).toBeVisible();
  if (process.env.CAGE_VISUAL_CHECK && role === "analyst") {
    await page.screenshot({ path: "/tmp/cage-overview.png", fullPage: true });
    const spec = await page.request.get("/v3/api-docs");
    expect(spec.status()).toBe(200);
    writeFileSync("../docs/openapi.json", await spec.text());
  }
}
test("cashier enters a transaction, reviews receipt, and cannot see SAR navigation", async ({
  page,
}) => {
  await login(page, "cashier");
  await expect(
    page.getByRole("button", { name: "SAR Manager", exact: false }),
  ).toHaveCount(0);
  await page
    .getByRole("button", { name: "Transaction Manager", exact: false })
    .click();
  await page.getByRole("button", { name: "+ Enter transaction" }).click();
  const dialog = page.getByRole("dialog", {
    name: "Enter transaction",
    exact: true,
  });
  await dialog.getByLabel("Beneficiary / patron").selectOption({ index: 1 });
  await dialog
    .getByLabel("Transaction type", { exact: true })
    .selectOption({ index: 1 });
  await dialog.getByLabel("Gross amount (USD)").fill("10001");
  await dialog.getByLabel("Actual cash component (USD)").fill("10001");
  await dialog
    .getByLabel("Marker / machine / account reference")
    .fill("DEMO-UI-" + Date.now());
  await dialog
    .getByLabel("Entry / correction reason")
    .fill("Synthetic browser smoke test posting");
  await dialog.getByRole("button", { name: "Confirm & save" }).click();
  await expect(dialog).not.toBeVisible();
  await page.getByRole("button", { name: /TX-/ }).first().click();
  await expect(
    page.getByRole("dialog", { name: "Record details" }),
  ).toContainText("$10,001.00");
  await expect(
    page.getByRole("button", { name: "Download demo receipt" }),
  ).toBeVisible();
});
test("analyst opens all report modules and audit versions", async ({
  page,
}) => {
  await login(page, "analyst");
  await page.getByRole("button", { name: "SAR Manager", exact: false }).click();
  await expect(
    page.getByRole("heading", { name: "SAR Manager", exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: /SAR-0/ }).first().click();
  await expect(
    page.getByRole("dialog", { name: "Record details" }),
  ).toContainText("Restricted synthetic narrative");
  await page
    .getByRole("button", { name: "Audit history", exact: false })
    .click();
  await expect(
    page.getByRole("dialog", { name: "Record details" }),
  ).toContainText("Synthetic");
});
test("administrator configuration has no SAR visibility and can clone a type", async ({
  page,
}) => {
  await login(page, "admin");
  await expect(
    page.getByRole("button", { name: "SAR Manager", exact: false }),
  ).toHaveCount(0);
  await page
    .getByRole("button", { name: "Setup Manager", exact: false })
    .click();
  await expect(
    page.getByRole("heading", { name: "Users & roles" }),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Clone", exact: true })
    .first()
    .click();
  await expect(
    page.getByRole("dialog", { name: "Clone transaction definition" }),
  ).toBeVisible();
  await page.getByLabel("Unique code").fill("UI_TEST_" + Date.now());
  await page
    .getByLabel("Compliance mapping rationale")
    .fill("Synthetic type clone for independent review");
  await page.getByRole("button", { name: "Confirm & save" }).click();
  await expect(
    page.getByRole("dialog", { name: "Clone transaction definition" }),
  ).not.toBeVisible();
});
test("tax specialist has a separate W-9 register", async ({ page }) => {
  await login(page, "tax");
  await page
    .getByRole("button", { name: "Tax Forms Manager", exact: false })
    .click();
  await page.getByRole("tab", { name: "W-9 Register" }).click();
  await expect(page.getByText("Certification", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Update W-9" }).first().click();
  await page.getByLabel("Certification state").selectOption("RECEIVED");
  await page
    .getByLabel("Reason", { exact: true })
    .fill("Synthetic certification received for UI smoke test");
  await page.getByRole("button", { name: "Confirm & save" }).click();
  await expect(
    page.getByRole("dialog", { name: "W-9 register · retained internally" }),
  ).not.toBeVisible();
});
test("mobile navigation stays usable", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await login(page, "cashier");
  await page.getByRole("button", { name: "Toggle navigation" }).click();
  await page
    .getByRole("button", { name: "Subject Manager", exact: false })
    .click();
  await expect(
    page.getByRole("heading", { name: "Subject Manager", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "+ Create patron" }),
  ).toBeVisible();
});

test("CTR maker, approver and filer complete a simulated acknowledgment", async ({
  page,
}) => {
  await login(page, "analyst");
  await page.getByRole("button", { name: "CTR Manager", exact: false }).click();
  await page.getByRole("button", { name: /CTR-0/ }).first().click();
  const detail = page.getByRole("dialog", { name: "Record details" });
  const heading = await detail.getByRole("heading").first().innerText();
  const reportId = heading.match(/#(\d+)/)![1];
  await detail.getByRole("button", { name: "Send for review" }).click();
  await page
    .getByLabel("Reason / resolution")
    .fill("Synthetic independent CTR review request");
  await page.getByRole("button", { name: "Confirm & save" }).click();
  await expect(
    page.getByRole("dialog", { name: "Record details" }),
  ).toContainText("PENDING REVIEW");
  await page.getByRole("button", { name: "Close details" }).click();
  await page.getByRole("button", { name: "Sign out" }).click();
  await login(page, "approver");
  await page.getByRole("button", { name: "CTR Manager", exact: false }).click();
  await page
    .getByRole("button", {
      name: "CTR-" + reportId.padStart(4, "0"),
      exact: true,
    })
    .click();
  await page.getByRole("button", { name: "Approve revision" }).click();
  await page
    .getByLabel("Reason / resolution")
    .fill("Synthetic independent approval after source review");
  await page.getByRole("button", { name: "Confirm & save" }).click();
  await expect(
    page.getByRole("dialog", { name: "Record details" }),
  ).toContainText("APPROVED");
  await page.getByRole("button", { name: "Close details" }).click();
  await page.getByRole("button", { name: "Sign out" }).click();
  await login(page, "filer");
  await page.getByRole("button", { name: "CTR Manager", exact: false }).click();
  await page
    .getByRole("checkbox", { name: "Select report " + reportId, exact: true })
    .check();
  await page.getByRole("button", { name: "Prepare batch (1)" }).click();
  await page
    .getByRole("button", { name: /MOCK-CTR-/ })
    .first()
    .click();
  await page.getByRole("button", { name: "Submit to simulator" }).click();
  await page
    .getByLabel("Reason", { exact: true })
    .fill("Synthetic authorized simulation submission");
  await page.getByRole("button", { name: "Confirm & save" }).click();
  await expect(
    page.getByRole("dialog", { name: "Record details" }),
  ).toContainText("AWAITING RESPONSE");
  await page
    .getByRole("button", { name: "Reconcile simulated response" })
    .click();
  await page
    .getByLabel("Reason", { exact: true })
    .fill("Synthetic acknowledgment reconciliation");
  await page.getByRole("button", { name: "Confirm & save" }).click();
  await expect(
    page.getByRole("dialog", { name: "Record details" }),
  ).toContainText("RESPONSE RECEIVED");
});
