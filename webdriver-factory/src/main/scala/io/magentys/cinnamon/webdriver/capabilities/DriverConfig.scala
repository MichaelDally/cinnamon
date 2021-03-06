package io.magentys.cinnamon.webdriver.capabilities

import com.typesafe.config.{ConfigObject, ConfigBeanFactory, Config, ConfigFactory}
import io.magentys.cinnamon.webdriver.Keys
import io.magentys.cinnamon.webdriver.remote.{CinnamonRemote, RemoterDetector}
import org.openqa.selenium.remote.DesiredCapabilities
import scala.collection.JavaConverters._
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

import scala.util.Try

case class DriverConfig(desiredCapabilities: DesiredCapabilities, requiresMoveMouse:Boolean)

object DriverConfig {
  /**
    * This class is responsible to define the webdriver config based on user options.
    * It combines Capabilities whether it's remote or not. Will automatically identify if remote is required,
    * based on the fact the user has passed a hubUrl.
    *
    * @param browserProfile user's selected browserProfile from capabilities-profiles
    * @param finalConfig capabilities-profiles config should be passed in order to bind the correct object based on the selected browserProfile
    * @param hubUrl if this is defined remote capabilities will also be added
    * @param defaultConfig this is passed to load default extras
    * @return DriverConfig object
    */
  def apply(browserProfile: String, finalConfig:Config, hubUrl: String, defaultConfig: Config):DriverConfig = {

    //1. Get the Capabilities profile config from the combined config
    val capabilitiesProfiles = finalConfig.getConfig(Keys.CAPABILITIES_PROFILES_KEY)

    //2. load basics
    val basicCapabilities = capabilitiesProfiles.as[BasicCapabilities](browserProfile)
    val basicCaps = new DesiredCapabilities(basicCapabilities.asMap.asJava)

    //3. driverExtras
    val extraCapabilities = {
      val userDriverExtras = getDriverExtrasWithProfile(browserProfile, finalConfig)
      val defaultDriverExtras = getDefaultDriverExtrasWithBrowserName(basicCapabilities.browserName, defaultConfig)
      val driverExtras = getDriverExtras(userDriverExtras, defaultDriverExtras)
      DriverExtrasBinder.bindExtrasMap(basicCapabilities.browserName, driverExtras)
    }
    val extraCaps = new DesiredCapabilities(extraCapabilities.getCapabilityMap.asJava)

    //4. merge them all and add remotes if required
    val capabilities: DesiredCapabilities = {
      if(remoteCapabilitiesRequired(hubUrl))
        basicCaps.merge(extraCaps).merge(remoteCapabilities(browserProfile, finalConfig, hubUrl))
      else
        basicCaps.merge(extraCaps)
    }

    DriverConfig(capabilities, extraCapabilities.requiresMoveMouse)
  }

  private[capabilities] def getDriverExtras(userDriverExtras: Option[Config], defaultDriverExtras: Option[Config]): Map[String, AnyRef] = {
      if (userDriverExtras.isDefined && defaultDriverExtras.isDefined) {
        val extras = userDriverExtras.get.withFallback(defaultDriverExtras.get)
        configToMap(extras)
      } else if (userDriverExtras.isDefined) {
        val extras = userDriverExtras.get
        configToMap(extras)
      } else if (defaultDriverExtras.isDefined) {
        val extras = defaultDriverExtras.get
        configToMap(extras)
      } else {
        Map.empty
      }
  }

  private[capabilities] def getDriverExtrasWithProfile(browserProfile: String, config: Config) = {
    Try(config.getConfig(browserProfile + "." + Keys.DRIVER_EXTRAS_KEY)).toOption
  }

  private[capabilities] def getDefaultDriverExtrasWithBrowserName (browserName: String, defaultConfig: Config) = {
    Try(defaultConfig
      .getConfig(Keys.CAPABILITIES_PROFILES_KEY +"."+ browserName + "." + Keys.DRIVER_EXTRAS_KEY))
      .toOption
  }

  private[capabilities] def configToMap(c:Config): Map[String, AnyRef] = {
    c.entrySet.asScala.map(f => (f.getKey, f.getValue.unwrapped())).toMap
  }

  private[capabilities] def remoteCapabilitiesRequired(hubUrl:String): Boolean = hubUrl != null && hubUrl.nonEmpty

  private[capabilities] def remoteCapabilities(userProfile: String, config:Config, url:String): DesiredCapabilities = {
    val remotersMatched: List[CinnamonRemote] = RemoterDetector.getRemoterMatchesURL(url)
    remotersMatched.size match {
      case 1 => remotersMatched.head.capabilities(userProfile, config)
      case x if x > 1 => throw new Exception("More than one remoter found with url: [" + url + "]")
      case _ => new DesiredCapabilities() // Defaults to selenium grid and provides empty capabilities to merge
    }
  }
}
