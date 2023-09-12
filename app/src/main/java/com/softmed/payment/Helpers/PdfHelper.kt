package com.softmed.payment.helpers

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.preference.PreferenceManager
import com.softmed.payment.R
import com.softmed.payment.storage.ClientesContract
import com.softmed.payment.storage.InvoiceItemsContract
import com.softmed.payment.storage.TransactionContract
import org.jetbrains.anko.AnkoLogger


class PdfHelper(private val ctx: Context) : AnkoLogger {
    companion object {
        private const val LETTER_PAGE_WIDTH = 595
        private const val LETTER_PAGE_HEIGHT = 842
        private const val TICKET_PAGE_WIDTH = 200
        private const val TICKET_PAGE_HEIGHT = 842
        private const val MARGIN_LEFT = 20f
        private const val MARGIN_RIGHT = 20f
        private const val MARGIN_TOP = 10f
        private const val LINE_SEPARATION = 15f
        private const val TITLE_LINE_SEPARATION = 20f
    }

    private val companyName by lazy {
        PreferenceManager.getDefaultSharedPreferences(ctx).getString(ctx.getString(R.string.pref_key_display_name), "")
    }
    private val companyNit by lazy {
        PreferenceManager.getDefaultSharedPreferences(ctx).getString(ctx.getString(R.string.pref_key_nit), "")
    }
    private val companyAddress by lazy {
        PreferenceManager.getDefaultSharedPreferences(ctx).getString(ctx.getString(R.string.pref_key_direction), "")
    }
    private val companyEmail by lazy {
        PreferenceManager.getDefaultSharedPreferences(ctx).getString(ctx.getString(R.string.pref_key_email), "")
    }
    private val companyPhone by lazy {
        PreferenceManager.getDefaultSharedPreferences(ctx).getString(ctx.getString(R.string.pref_key_telephone_number), "")
    }

    private var linePosition = 0f

    fun createBillTickerPdf(subtotal: Double,
                            iva: Double,
                            total: Double,
                            items: List<InvoiceItemsContract.InvoiceItems>,
                            payment: TransactionContract.Transaction,
                            client: ClientesContract.Cliente?,
                            resources: Resources? = null): PdfDocument {

        val resource = resources ?: ctx.resources
        val baseHeight = 300
        val ticketHeight = baseHeight + (items.size * 80)
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(LETTER_PAGE_WIDTH, ticketHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        linePosition = 0f
        centerOnCanvas(canvas, companyName!!)
        centerOnCanvas(canvas, companyAddress!!)
        centerOnCanvas(canvas, "NIT: $companyNit")
        centerOnCanvas(canvas, "Teléfono: $companyPhone")
        centerOnCanvas(canvas, companyEmail!!)
        writeTitleOnCanvas(canvas, resource.getString(R.string.bill_ticket))

        val date = resource.getString(R.string.bill_ticket_pdf_date, payment.paymentDate)
        canvas.drawText(date, MARGIN_LEFT, newLinePosition(TITLE_LINE_SEPARATION), paint)

        val fullname = resource.getString(R.string.bill_ticket_pdf_client_fullname, client?.name ?: "", client?.lastname ?: "")
        canvas.drawText(fullname, MARGIN_LEFT, newLinePosition(), paint)

        val nit = resource.getString(R.string.bill_ticket_pdf_client_nit, client?.nit ?: "")
        canvas.drawText(nit, MARGIN_LEFT, newLinePosition(), paint)

        val phone = resource.getString(R.string.bill_ticket_pdf_client_phone, client?.phoneNumber ?: "")
        canvas.drawText(phone, MARGIN_LEFT, newLinePosition(), paint)

        val billNumber = resource.getString(R.string.bill_ticket_pdf_invoice_number, payment.invoiceNumber)
        canvas.drawText(billNumber, MARGIN_LEFT, newLinePosition(20f), paint)

        writeItemsOnCanvas(canvas, items, resource, payment)
        writePaymentOnCanvas(canvas, payment, subtotal, iva, total, resource)
        document.finishPage(page)

        return document
    }

    private fun centerOnCanvas(canvas: Canvas, text: String) {
        val paint = Paint()
        val canvasWidth = canvas.width
        val textWidth = paint.measureText(text)
        val startX = ((canvasWidth - textWidth) / 2)

        canvas.drawText(text, startX, newLinePosition(), paint)
    }

    private fun getStringForPaymentsMethod(paymentType: Int, resource: Resources) : String {
        return when(paymentType) {
            TransactionContract.PaymentMethods.Cash.ordinal -> resource.getString(R.string.payment_method_cash)
            TransactionContract.PaymentMethods.Card.ordinal -> resource.getString(R.string.payment_method_card)
            TransactionContract.PaymentMethods.Credit.ordinal -> resource.getString(R.string.payment_method_credit)
            else -> ""
        }
    }

    private fun newLinePosition(extra: Float = 0f): Float {
        linePosition += LINE_SEPARATION + extra
        return linePosition

    }

    private fun writeItemsOnCanvas(canvas: Canvas, items: List<InvoiceItemsContract.InvoiceItems>, resource: Resources,payment: TransactionContract.Transaction) {
        val paint = Paint()
        val leftWidth = 150
        val leftSide = MARGIN_LEFT
        val rightSide = canvas.width - MARGIN_RIGHT - leftWidth

        for (i in items.indices){
            var positionY = newLinePosition(10f)
            canvas.drawText(items[i].itemName, leftSide, positionY, paint)
            val value = resource.getString(R.string.bill_ticket_pdf_value, items[i].itemPrice)
            canvas.drawText(value, rightSide, positionY, paint)

            positionY = newLinePosition()
            val iva = resource.getString(R.string.bill_ticket_pdf_iva, items[i].itemIva)
            canvas.drawText(iva, leftSide, positionY, paint)

            positionY = newLinePosition()
            val discount = resource.getString(R.string.bill_ticket_pdf_discount, items[i].itemDiscount)
            canvas.drawText(discount, rightSide, positionY, paint)

            positionY = newLinePosition()
            val amount = resource.getString(R.string.bill_ticket_pdf_amount, items[i].itemAmount)
            canvas.drawText(amount, rightSide, positionY, paint)

            positionY = newLinePosition()
            val pass = resource.getString(R.string.bill_credit_deposit, (payment.paymentCreditDeposit))
            canvas.drawText(pass, rightSide, positionY, paint)
        }
    }

    private fun writePaymentOnCanvas(canvas: Canvas,
                                     payment: TransactionContract.Transaction,
                                     subtotal: Double,
                                     iva: Double,
                                     total: Double,
                                     resource: Resources) {
        val paint = Paint()
        val leftSide = MARGIN_LEFT
        val rightSide = (canvas.width / 2).toFloat()

        val line1 = newLinePosition(20f)
        val subtotalText = resource.getString(R.string.bill_ticket_pdf_subtotal, subtotal)
        canvas.drawText(subtotalText, leftSide, line1, paint)
        val methodPayment = resource.getString(R.string.bill_ticket_pdf_payment_method, getStringForPaymentsMethod(payment.paymentType, resource))
        canvas.drawText(methodPayment, rightSide, line1, paint)

        val line2 = newLinePosition()
        val ivaText = resource.getString(R.string.bill_ticket_pdf_iva_total, iva)
        canvas.drawText(ivaText, leftSide, line2, paint)

        val line3 = newLinePosition()
        val totalText = resource.getString(R.string.bill_ticket_pdf_total_bill, total)
        canvas.drawText(totalText, leftSide, line3, paint)

        when (payment.paymentType) {
            TransactionContract.PaymentMethods.Cash.ordinal -> {
                val cash = resource.getString(R.string.bill_ticket_pdf_cash_total, payment.paymentTotal)
                canvas.drawText(cash, rightSide, line2, paint)

                val change = resource.getString(R.string.bill_ticket_pdf_change_total, (payment.paymentTotal - payment.invoiceTotal))
                canvas.drawText(change, rightSide, line3, paint)
            }
            TransactionContract.PaymentMethods.Card.ordinal -> {
                val reference = resource.getString(R.string.bill_ticket_pdf_reference_number, payment.paymentCardReference)
                canvas.drawText(reference, rightSide, line2, paint)
            }
        }

    }

    private fun writeTitleOnCanvas(canvas: Canvas, text: String) {
        val paint = Paint()
        paint.textSize = 20f

        val rect = Rect()
        paint.getTextBounds(text, 0 , text.length , rect)
        val canvasWidth = canvas.width
        val textWidth = rect.width()
        val startX = ((canvasWidth - textWidth) / 2).toFloat()

        canvas.drawText(text.toUpperCase(), startX, newLinePosition(TITLE_LINE_SEPARATION), paint)
    }
}