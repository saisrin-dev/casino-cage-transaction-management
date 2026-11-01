import {test,expect} from '@playwright/test';
test('open demo, dedicated entry, receipt, refresh persistence and reset',async({page})=>{
  await page.goto('/');await expect(page.getByRole('heading',{name:'Every detail. In view.'})).toBeVisible();
  await expect(page.getByRole('textbox',{name:'Password',exact:true})).toHaveCount(0);
  await page.getByRole('button',{name:'New transaction',exact:false}).click();
  await page.getByLabel('Transaction patron',{exact:true}).selectOption({label:'P-0012 · Skyler Pine [Synthetic]'});
  await page.getByLabel('Transaction type',{exact:true}).selectOption({label:'Chip purchase'});
  await page.getByLabel('Transaction gross amount').fill('11000');await page.getByLabel('Transaction cash amount').fill('11000');await page.getByLabel('Transaction reference').fill('DEMO-UI-TEST');
  await page.getByRole('button',{name:'Save transaction',exact:false}).click();await expect(page.getByText('SAVED SUCCESSFULLY')).toBeVisible();
  await page.getByRole('button',{name:'View record & receipt',exact:false}).click();await expect(page.getByRole('dialog')).toBeVisible();
  await page.reload();await page.getByRole('button',{name:'Transaction Manager',exact:false}).click();await page.getByLabel('Search records').fill('DEMO-UI-TEST');await expect(page.getByRole('button',{name:/TX-00/})).toHaveCount(1);
  await page.getByRole('button',{name:'Reset demo',exact:true}).click();await page.getByLabel('Type RESET to restore sample data').fill('RESET');await page.getByRole('button',{name:'Confirm & save',exact:true}).click();
});
test('mobile entry and navigation stay within viewport',async({page})=>{await page.setViewportSize({width:390,height:844});await page.goto('/');await page.getByRole('button',{name:'New transaction',exact:false}).click();await expect(page.getByRole('heading',{name:'Enter a transaction'})).toBeVisible();expect(await page.evaluate(()=>document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);});
