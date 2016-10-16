/*The MIT License (MIT)

Copyright (c) 2013 Nicholas Marshall

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.*/

/*This class originates in the desktop-pushbullet project
(https://github.com/nmarshall23/desktop-pushbullet), but has been modified to
work with this project (https://github.com/hnrklssn/units-toolbox). This source
file is subject to the above license, however that does not extend to the rest
of the project, which is licensed under the GNU General Public License.*/

import org.streum.configrity._
import org.streum.configrity.io.StandardFormat.ParserException
import Option.{apply => ?}
import java.io.File
import scalaz.IsEmpty

object Preferences {

  //Damnit can't think of away handle updates without making this a var.
  private var config = try {
    Configuration.load(ConfigName, org.streum.configrity.io.BlockFormat)
  } catch {
    //case e:java.io.FileNotFoundException => Configuration()
    case e:ParserException => Configuration() //Hmm Guess it's better to overwrite the configuration.
  }

  def getPBChannel: String = {
    config[String]("channel_tag")
  }

  def getPBChannel_Option: Option[String] = {
    config.get[String]("channel_tag")
  }

  def setPBChannel(tag: String): Boolean = {
    config = config.set[String]("channel_tag", tag)
    config.save(ConfigName)
    config.contains("channel_tag")
  }

  def isPushbulletActive(): Boolean = {
    config[Boolean]("pb-active")
  }

  def activatePushbullet(set_unset: Boolean): Boolean = {
    config = config.set[Boolean]("pb-active", set_unset)
    config.save(ConfigName)
    config[Boolean]("pb-active")
  }

  def setAPI_KEY(key:String):Boolean = {
    config = config.set[String]("users_api_key",key)
    config.save(ConfigName)
    config.contains("users_api_key")
  }

  /*def setDevice(device:Devices):Boolean = { //not used
    val manufacturer = device.extra.manufacturer
    val model = device.extra.model
    val name = s""""$manufacturer $model""""
    config = config.set[Int]("device.id"  , device.id)
       .set[String]("device.name", name )

    config.save(ConfigName)
    config.contains("device.id")

  }*/

  def NeedtoConfigure:Boolean = {
    val keys = List("users_api_key", "device.id","device.name")
    val keycheck = keys.forall( key => config.contains(key) ) //should be true
    !keycheck
  }

  def API_KEY = config[String]("users_api_key")
  def API_KEY_Option = config.get[String]("users_api_key")

  /*def DefaultDeviceId = config[Int]("device.id",0)
  def DefaultDeviceId_Option = config.get[Int]("device.id")
  def DefaultDeviceName = config[String]("device.name")*/

  private lazy val ConfigName = {
   val configFileName = "units-toolbox.conf"
   val userHomeDir = ?(System.getenv("USERPROFILE") ).getOrElse( ?(System.getProperty("user.home") ).getOrElse(".") )
   val ourConfDir = System.getProperty("os.name") match {
      case "Linux" => ".local" + File.separator + "share" + File.separator + "units-toolbox"
      case _ => ".units-toolbox"
   }

   val configDir =  userHomeDir + File.separator + ourConfDir

   def checkConfigDirExists(configDir:File):Boolean = configDir.isDirectory() || configDir.mkdirs()

   //println("Config Name: " + configDir)
   //println("Config dir: " + checkConfigDirExists(new File(configDir)) )
   //Not sure what else to do if configdir doesn't exists. Just return the configFileName
   if(checkConfigDirExists(new File(configDir)) ) configDir + File.separator + configFileName
   else configFileName

  }
}
