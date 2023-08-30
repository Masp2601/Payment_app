package com.softmed.payment.helpers

import android.content.Context
import com.softmed.payment.R
import com.softmed.payment.storage.*
import org.apache.poi.hssf.usermodel.HSSFSheet
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.error
import java.io.FileOutputStream


class ReportExcelHelpers(val ctx: Context) : AnkoLogger {

    fun createReportPayments(payments: List<TransactionContract.Transaction>, fileOutputStream: FileOutputStream?) : Boolean {
        if (fileOutputStream == null) return false
        try {
            val wb = HSSFWorkbook()
            val sheet: HSSFSheet = wb.createSheet(ctx.getString(R.string.sheet_payments))

            createRowTitle(sheet, 0,
                    ctx.getString(R.string.cell_title_bill_number),
                    ctx.getString(R.string.cell_title_bill_date),
                    ctx.getString(R.string.cell_title_bill_total),
                    ctx.getString(R.string.cell_title_bill_method_payment),
                    ctx.getString(R.string.cell_title_bill_reference_number))

            for (i in 0..(payments.size - 1)) {
                val row = sheet.createRow(i + 1)

                row.createCell(0).setCellValue(String.format("%010d", payments[i].invoiceNumber))
                row.createCell(1).setCellValue(DateTimeHelper.parseStringToDate(payments[i].paymentDate))
                row.createCell(2).setCellValue(payments[i].invoiceTotal)

                val cell = row.createCell(3)
                when (payments[i].paymentType) {
                    0 -> cell.setCellValue(ctx.getString(R.string.payment_method_cash))
                    1 -> cell.setCellValue(ctx.getString(R.string.payment_method_card))
                    2 -> cell.setCellValue(ctx.getString(R.string.payment_method_credit))
                }

                row.createCell(4).setCellValue(payments[i].paymentCardReference)
            }

            wb.write(fileOutputStream)
            fileOutputStream.close()

            return true
        } catch (e: Exception) {
            error(e.message)
            return false
        }
    }

    fun reportInvoices(invoices: List<InvoiceContract.Invoice>, fileOutputStream: FileOutputStream) : Boolean {
        try {
            val wb = HSSFWorkbook()
            val sheet: HSSFSheet = wb.createSheet()
            createRowTitle(sheet, 0,
                    ctx.getString(R.string.cell_title_bill_number),
                    ctx.getString(R.string.cell_title_bill_date),
                    ctx.getString(R.string.cell_title_bill_client_name),
                    ctx.getString(R.string.cell_title_bill_subtotal),
                    ctx.getString(R.string.cell_title_bill_iva),
                    ctx.getString(R.string.cell_title_bill_total))

            for (i in 0 until invoices.size) {
                val row = sheet.createRow(i + 1)
                row.createCell(0).setCellValue(String.format("%010d", invoices[i].invoiceNumber))
                row.createCell(1).setCellValue(invoices[i].date)
                row.createCell(2).setCellValue(invoices[i].clientName)
                row.createCell(3).setCellValue(invoices[i].subTotal)
                row.createCell(4).setCellValue(invoices[i].iva)
                row.createCell(5).setCellValue(invoices[i].total)
            }

            wb.write(fileOutputStream)
            fileOutputStream.close()

            return true
        } catch (e: Exception) {
            error(e.message)
            return false
        }
    }

    fun reportResumeItemsSold(items: List<InvoiceItemsContract.InvoiceItemsTotalResumen>,
                              details: List<InvoiceItemsContract.InvoiceItems>?,
                              fileOutputStream: FileOutputStream) : Boolean {
        try {
            val wb = HSSFWorkbook()
            val sheet: HSSFSheet = wb.createSheet(ctx.getString(R.string.resume_items_sold_sheet_name))
            createRowTitle(sheet, 0,
                    ctx.getString(R.string.cell_title_resume_items_sold_name),
                    ctx.getString(R.string.cell_title_resume_items_sold_amount),
                    ctx.getString(R.string.cell_title_resume_items_sold_total_iva),
                    ctx.getString(R.string.cell_title_resume_items_sold_total_discount),
                    ctx.getString(R.string.cell_title_resume_items_sold_total_paid)
            )

            for (i in 0 until items.size) {
                val row = sheet.createRow(i + 1)
                row.createCell(0).setCellValue(items[i].itemName)
                row.createCell(1).setCellValue("${items[i].itemAmountTotal}")
                row.createCell(2).setCellValue("${items[i].itemIvaTotal}")
                row.createCell(3).setCellValue("${items[i].itemDiscountTotal}")
                row.createCell(4).setCellValue("${items[i].itemPriceTotal}")
            }

            if (details != null) {
                val detailsSheet: HSSFSheet = wb.createSheet(ctx.getString(R.string.bill_items_sheet_name))
                createRowTitle(detailsSheet, 0,
                        ctx.getString(R.string.cell_title_sales_bill_number),
                        ctx.getString(R.string.cell_title_bill_item_name),
                        ctx.getString(R.string.cell_title_bill_item_price),
                        ctx.getString(R.string.cell_title_bill_item_iva),
                        ctx.getString(R.string.cell_title_bill_item_discount),
                        ctx.getString(R.string.cell_title_bill_item_amount)
                )

                for (i in 0 until details.size) {
                    val row = detailsSheet.createRow(i + 1)
                    row.createCell(0).setCellValue(String.format("%010d", details[i].invoiceNumber))
                    row.createCell(1).setCellValue(details[i].itemName)
                    row.createCell(2).setCellValue(details[i].itemPrice)
                    row.createCell(3).setCellValue(details[i].itemIva)
                    row.createCell(4).setCellValue(details[i].itemDiscount)
                    row.createCell(5).setCellValue(details[i].itemAmount.toDouble())
                }
            }

            wb.write(fileOutputStream)
            fileOutputStream.close()

            return true
        } catch (e: Exception) {
            error(e.message)
            return false
        }
    }

    private fun createRowTitle(sheet: HSSFSheet ,rowNum: Int, vararg titles: String) {
        val row = sheet.createRow(rowNum)
        for (i in 0..(titles.size - 1)){
            row.createCell(i).setCellValue(titles[i])
        }
    }

    private fun saveExcelFile(wb: HSSFWorkbook, filename: String){
        val fileStream = FilesHelper(ctx).getOutputStream(filename)

        if (fileStream != null) {
            wb.write(fileStream)
            fileStream.close()
        }
    }

    fun reportPurchases(expenseList: List<ExpenseContract.Expense>, outputStream: FileOutputStream): Boolean {
        try {
            val wb = HSSFWorkbook()
            val sheet: HSSFSheet = wb.createSheet()
            createRowTitle(sheet, 0,
                    ctx.getString(R.string.cell_title_purchase_order_number),
                    ctx.getString(R.string.cell_title_purchase_provider_name),
                    ctx.getString(R.string.cell_title_purchase_date),
                    ctx.getString(R.string.cell_title_purchase_total_paid),
                    ctx.getString(R.string.cell_title_purchase_observation)
            )

            for (i in 0 until expenseList.size) {
                val row = sheet.createRow(i + 1)
                row.createCell(0).setCellValue("${expenseList[i].orderNumber}")
                row.createCell(1).setCellValue(expenseList[i].providerName)
                row.createCell(2).setCellValue(expenseList[i].date)
                row.createCell(3).setCellValue(expenseList[i].totalOrder)
                row.createCell(4).setCellValue(expenseList[i].observation)
            }

            wb.write(outputStream)
            outputStream.close()

            return true
        } catch (e: Exception) {
            error(e.message)
            return false
        }
    }

    fun createClientList(clients: List<ClientesContract.Cliente>): HSSFWorkbook? {
        try {
            val wb = HSSFWorkbook()
            val sheet: HSSFSheet = wb.createSheet(ctx.getString(R.string.clients_sheet_name))
            createRowTitle(sheet, 0,
                    ctx.getString(R.string.cell_title_client_name),
                    ctx.getString(R.string.cell_title_client_lastname),
                    ctx.getString(R.string.cell_title_client_nit),
                    ctx.getString(R.string.cell_title_client_email),
                    ctx.getString(R.string.cell_title_client_phone_number),
                    ctx.getString(R.string.cell_title_client_direction)
            )

            for (i in 0 until clients.size) {
                val row = sheet.createRow(i + 1)
                row.createCell(0).setCellValue(clients[i].name)
                row.createCell(1).setCellValue(clients[i].lastname)
                row.createCell(2).setCellValue(clients[i].nit)
                row.createCell(3).setCellValue(clients[i].email)
                row.createCell(4).setCellValue(clients[i].phoneNumber)
                row.createCell(4).setCellValue(clients[i].direction)
            }

            return wb
        } catch (e: Exception) {
            error(e.message)
            return null
        }
    }

    fun createClientList(clients: List<ClientesContract.Cliente>, outputStream: FileOutputStream): Boolean {
        try {
            val wb = createClientList(clients) ?: return false

            wb.write(outputStream)
            outputStream.close()

            return true
        } catch (e: Exception) {
            error(e.message)
            return false
        }
    }

    fun createServiceList(services: List<ServiciosContract.Service>): HSSFWorkbook? {
        try {
            val wb = HSSFWorkbook()
            val sheet: HSSFSheet = wb.createSheet(ctx.getString(R.string.service_sheet_name))
            createRowTitle(sheet, 0,
                    ctx.getString(R.string.cell_title_service_name),
                    ctx.getString(R.string.cell_title_service_description),
                    ctx.getString(R.string.cell_title_service_price),
                    ctx.getString(R.string.cell_title_service_iva),
                    ctx.getString(R.string.cell_title_service_discount)
            )

            for (i in 0 until services.size) {
                val row = sheet.createRow(i + 1)
                row.createCell(0).setCellValue(services[i].name)
                row.createCell(1).setCellValue(services[i].description)
                row.createCell(2).setCellValue(services[i].price)
                row.createCell(3).setCellValue(services[i].iva)
                row.createCell(4).setCellValue(services[i].discount)
            }

            return wb
        } catch (e: Exception) {
            error(e.message)
            return null
        }
    }

    fun createSalesList(sales: List<InvoiceContract.Invoice>, items: List<InvoiceItemsContract.InvoiceItems>, payments: List<TransactionContract.Transaction>): HSSFWorkbook? {
        try {
            val wb = HSSFWorkbook()
            val sheet: HSSFSheet = wb.createSheet(ctx.getString(R.string.sales_sheet_name))
            createRowTitle(sheet, 0,
                    ctx.getString(R.string.cell_title_sales_bill_number),
                    ctx.getString(R.string.cell_title_sales_client_name),
                    ctx.getString(R.string.cell_title_sales_subtotal),
                    ctx.getString(R.string.cell_title_sales_iva),
                    ctx.getString(R.string.cell_title_sales_discount),
                    ctx.getString(R.string.cell_title_sales_total),
                    ctx.getString(R.string.cell_title_sales_date),
                    ctx.getString(R.string.cell_title_payment_method),
                    ctx.getString(R.string.cell_title_payment_reference_number),
                    ctx.getString(R.string.cell_title_sales_nullify)
            )

            for (i in 0 until sales.size) {
                val row = sheet.createRow(i + 1)
                row.createCell(0).setCellValue(String.format("%010d", sales[i].invoiceNumber))
                row.createCell(1).setCellValue(sales[i].clientName)
                row.createCell(2).setCellValue(sales[i].subTotal)
                row.createCell(3).setCellValue(sales[i].iva)
                row.createCell(4).setCellValue(sales[i].discountValue)
                row.createCell(5).setCellValue(sales[i].total)
                row.createCell(6).setCellValue(sales[i].date)

                val payment = payments.find { it.invoiceNumber == sales[i].invoiceNumber }

                val paymentMethod = when(payment?.paymentType) {
                    0 -> ctx.getString(R.string.payment_method_cash)
                    1 -> ctx.getString(R.string.payment_method_card)
                    2 -> ctx.getString(R.string.payment_method_credit)
                    else -> ""
                }
                row.createCell(7).setCellValue(paymentMethod)
                row.createCell(8).setCellValue(payment?.paymentCardReference ?: "")

                row.createCell(9).setCellValue(getNullifyString(sales[i].nullify))
            }

            val itemsSheet: HSSFSheet = wb.createSheet(ctx.getString(R.string.bill_items_sheet_name))
            createRowTitle(itemsSheet, 0,
                    ctx.getString(R.string.cell_title_sales_bill_number),
                    ctx.getString(R.string.cell_title_bill_item_name),
                    ctx.getString(R.string.cell_title_bill_item_price),
                    ctx.getString(R.string.cell_title_bill_item_iva),
                    ctx.getString(R.string.cell_title_bill_item_discount),
                    ctx.getString(R.string.cell_title_bill_item_amount)
            )

            for (i in 0 until items.size) {
                val row = itemsSheet.createRow(i + 1)
                row.createCell(0).setCellValue(String.format("%010d", items[i].invoiceNumber))
                row.createCell(1).setCellValue(items[i].itemName)
                row.createCell(2).setCellValue(items[i].itemPrice)
                row.createCell(3).setCellValue(items[i].itemIva)
                row.createCell(4).setCellValue(items[i].itemDiscount)
                row.createCell(5).setCellValue(items[i].itemAmount.toDouble())
            }

            return wb
        } catch (e: Exception) {
            error(e.message)
            return null
        }
    }

    fun createExpensesList(expenses: List<ExpenseContract.Expense>) : HSSFWorkbook? {
        try {
            val wb = HSSFWorkbook()
            val sheet: HSSFSheet = wb.createSheet(ctx.getString(R.string.expenses_sheet_name))
            createRowTitle(sheet, 0,
                    ctx.getString(R.string.cell_title_expenses_order_number),
                    ctx.getString(R.string.cell_title_expenses_provider),
                    ctx.getString(R.string.cell_title_expenses_observation),
                    ctx.getString(R.string.cell_title_expenses_total_order),
                    ctx.getString(R.string.cell_title_expenses_date)
            )

            for (i in 0 until expenses.size) {
                val row = sheet.createRow(i + 1)
                row.createCell(0).setCellValue(expenses[i].orderNumber.toString())
                row.createCell(1).setCellValue(expenses[i].providerName)
                row.createCell(2).setCellValue(expenses[i].observation)
                row.createCell(3).setCellValue(expenses[i].totalOrder)
                row.createCell(4).setCellValue(expenses[i].date)
            }

            return wb
        } catch (e: Exception) {
            error(e.message)
            return null
        }
    }

    private fun getNullifyString(nullify: Int): String = if (nullify == 0) "No" else "Sí"
}