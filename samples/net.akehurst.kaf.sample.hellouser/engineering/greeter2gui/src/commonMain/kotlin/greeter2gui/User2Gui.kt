package net.akehurst.kaf.simple.hellouser.engineering.greeter2gui

import com.soywiz.korge.Korge
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.solidRect
import com.soywiz.korim.color.Colors
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korio.async.runBlockingNoSuspensions
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.akehurst.kaf.sample.hellouser.greeter.api.Credentials
import net.akehurst.kaf.sample.hellouser.greeter.api.GreeterNotification
import net.akehurst.kaf.sample.hellouser.greeter.api.GreeterRequest
import net.akehurst.kaf.sample.hellouser.greeter.api.Message
import kotlin.reflect.KClass

class Greeter2Gui : GreeterNotification {

    lateinit var greeterRequest: GreeterRequest

    val startScene = StartScene()
    val loginScene = LoginScene()
    val messageScene = MessageScene()

    val module = object : Module() {
        override val title = "Hello User"
        override val mainScene: KClass<out Scene> = StartScene::class

        override suspend fun init(injector: AsyncInjector): Unit = injector.run {
            mapPrototype { startScene }
            mapPrototype { loginScene }
            mapPrototype { messageScene }
        }
    }


    fun start() {
        GlobalScope.launch {
            Korge(Korge.Config(module = module))
        }
    }

    // --- GreeterNotification ---

    override fun started() {
        runBlockingNoSuspensions {
            this.startScene.sceneContainer.changeTo<LoginScene>()   //TODO: why can't I changeTo(loginScene)
        }
    }

    override fun sendMessage(message: Message) {
        this.messageScene.text = message.value
    }
}