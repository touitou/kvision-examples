package com.example

import com.github.andrewoma.kwery.core.ThreadLocalSession
import com.github.andrewoma.kwery.core.interceptor.LoggingInterceptor
import com.typesafe.config.Config
import org.jooby.Jooby.run
import org.jooby.Kooby
import org.jooby.jdbc.Jdbc
import org.jooby.pac4j.Pac4j
import org.jooby.require
import org.pac4j.core.credentials.UsernamePasswordCredentials
import org.pac4j.http.client.indirect.FormClient
import pl.treksoft.kvision.remote.applyRoutes
import pl.treksoft.kvision.remote.kvisionInit
import javax.sql.DataSource

class App : Kooby({
    kvisionInit()
    use(Jdbc("db"))
    applyRoutes(RegisterProfileServiceManager)
    use(Pac4j().client { _ ->
        FormClient("/") { credentials, context ->
            require(MyDbProfileService::class).validate(credentials as UsernamePasswordCredentials, context)
        }
    })
    applyRoutes(AddressServiceManager)
    applyRoutes(ProfileServiceManager)
    onStart {
        val db = require("db", DataSource::class)
        val session = ThreadLocalSession(db, getDbDialect(require(Config::class)), LoggingInterceptor())
        try {
            AddressDao(session).findById(1)
        } catch (e: Exception) {
            val schema = this.javaClass.getResource("/schema.sql").readText()
            session.update(schema)
        }
    }
})

fun main(args: Array<String>) {
    run(::App, args)
}
