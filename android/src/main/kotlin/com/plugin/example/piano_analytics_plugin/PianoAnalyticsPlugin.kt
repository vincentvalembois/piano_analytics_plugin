package com.plugin.example.piano_analytics_plugin

import android.content.Context
import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.piano.android.analytics.Configuration

import io.piano.android.analytics.PianoAnalytics
import io.piano.android.analytics.model.Event
import io.piano.android.analytics.model.Property
import io.piano.android.analytics.model.PropertyName
import io.piano.android.analytics.model.VisitorIDType

object PAEvents {
  const val SET_CONFIGURATION = "setConfiguration"
  const val SEND_EVENT = "sendEvent"
}

/** PianoAnalyticsPlugin */
class PianoAnalyticsPlugin: FlutterPlugin, MethodCallHandler {

  private val pa: PianoAnalytics
    get() = PianoAnalytics.getInstance()

  private lateinit var channel : MethodChannel

  private lateinit var context: Context

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    context = flutterPluginBinding.applicationContext
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "piano_analytics_plugin")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    try {

      val arguments: HashMap<String, Any> = call.arguments as HashMap<String, Any>

      when (call.method) {
        PAEvents.SET_CONFIGURATION -> {
          val collectDomain = arguments["collectDomain"] as? String
          val site = arguments["site"] as? Int

          if (collectDomain?.isEmpty() != false || site == null) {
            result.error("500", "collectDomain is required", null)
          } else {
            val visitorID = arguments["visitorID"] as? String

            val configuration = Configuration.Builder(
              collectDomain = collectDomain,
              site = site,
              visitorIDType = if (visitorID?.isEmpty() == false) { VisitorIDType.CUSTOM } else { VisitorIDType.UUID }
            )

            PianoAnalytics.init(context, configuration.build())

            if (visitorID?.isEmpty() == false) {
              pa.customVisitorId = visitorID
            }
          }
        }

        PAEvents.SEND_EVENT -> {
          val eventName = arguments["eventName"] as? String

          if (eventName?.isNullOrEmpty() ?: true) {
            result.error("500", "eventName is required", null)
          } else {
            val data: HashMap<String, Any?> = arguments["data"] as HashMap<String, Any?>
            val event = Event.Builder(eventName ?: "").properties(data.toProperties())
            pa.sendEvents(event.build())
          }
        }

        else -> result.notImplemented()
      }
    } catch (e: Exception) {
      result.error("500", e.toString(), null)
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }
}

fun HashMap<String, Any?>.toProperties(): Collection<Property> = map { (key, value) ->
  Property(PropertyName(key), value.toString())
}
