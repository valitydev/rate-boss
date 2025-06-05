package dev.vality.rateboss.servlet

import dev.vality.exrates.service.ExchangeRateServiceSrv
import dev.vality.woody.thrift.impl.http.THServiceBuilder
import jakarta.servlet.*
import jakarta.servlet.annotation.WebServlet

@WebServlet("/ex-rate")
class ExRateServlet(
    private val serverHandler: ExchangeRateServiceSrv.Iface,
) : GenericServlet() {
    private lateinit var servlet: Servlet

    override fun init(config: ServletConfig) {
        super.init(config)
        servlet = THServiceBuilder().build(ExchangeRateServiceSrv.Iface::class.java, serverHandler)
    }

    override fun service(
        request: ServletRequest,
        response: ServletResponse,
    ) {
        servlet.service(request, response)
    }
}
