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
work in this project (https://github.com/hnrklssn/units-toolbox). This source
file is subject to the above license, however that does not extend to the rest
of the project, which uses the GNU General Public License.*/


import scala.collection.immutable.Map
import akka.actor.Actor
import dispatch._, Defaults._
import argonaut._, Argonaut._
import scala.util.Success
import scala.util.Failure
//import desktoppushbullet.QuitWithError

abstract class PushableType extends ApiCall
case class pushNote(title: String, body: String) extends PushableType
case class pushLink(title: String, url: String) extends PushableType
case class pushAddress(name: String, address: String) extends PushableType
case class pushToDoList(tile: String, items: String) extends PushableType
case class pushFile(file: String) extends PushableType

abstract class ApiCall
//case class GetDevices(API_KEY: String) extends ApiCall

class PushAPI extends Actor {
  lazy val key = Preferences.API_KEY
  lazy val pushAPI = new PushbulletAPI(key)
  lazy val channelTag = Preferences.getPBChannel

  def receive = {
    //case GetDevices => saveDevice //not used as we push to channels, not devices
    case pushToDoList(title, items) =>
    case pushFile(file) => pushAPI.push(channelTag, Map("type" -> "file", "file" -> file))
    case pushNote(title, body) => pushAPI.push(channelTag, Map("type" -> "note", "title" -> title, "body" -> body))
    case pushLink(title, url) => pushAPI.push(channelTag, Map("type" -> "link", "title" -> title, "url" -> url)) //this is the one we use
    case pushAddress(name, address) => pushAPI.push(channelTag, Map("type" -> "address", "name" -> name, "address" -> address))
  }

  /*def saveDevice = pushAPI.getDevices onComplete {
    case Success(json) => {

      for (
        listOf <- json.decodeOption[ListOfDevices];
        first <- listOf.devices.headOption
      ) Preferences.setDevice(first)

      PushBulletApp.mainloop ! StartAPP
    }
    case Failure(t) => {
      println("An error has occured: " + t.getMessage)

      PushBulletApp.mainloop ! QuitWithError(t.getMessage)
    }
  }*/

}

/*case class ListOfDevices(devices: List[Devices], shared_devices: List[Devices])
case class Devices(id: Int, extra: DeviceExtraInfo)
case class DeviceExtraInfo(android_version: String, app_version: String, manufacturer: String, model: String, sdk_version: String)

object ListOfDevices {
  implicit def ListOfDevicesCodecJson: CodecJson[ListOfDevices] =
    casecodec2(ListOfDevices.apply, ListOfDevices.unapply)("devices", "shared_devices")
}

object DeviceExtraInfo {
  implicit def ExtraInfoCodecJson: CodecJson[DeviceExtraInfo] =
    casecodec5(DeviceExtraInfo.apply, DeviceExtraInfo.unapply)("android_version", "app_version", "manufacturer", "model", "sdk_version")
}

object Devices {
  implicit def DevicesCodecJson: CodecJson[Devices] =
    casecodec2(Devices.apply, Devices.unapply)("id", "extras")
}*/

class PushbulletAPI(API_KEY: String) {

  type StatusCode = Int
  private val apiURL = :/("api.pushbullet.com").secure.as_!(API_KEY, "foo") / "v2"
  private val devicesAPI = apiURL / "devices"
  private val pushAPI = apiURL / "pushes"

  //Probably won't be used in this application, but I'll keep it for posterity
  /*def getDevices = {

    val devices = Http(devicesAPI.GET OK as.String)
    // for(d <- devices() ) println(d)
    //  result.status
    devices
  }*/

  /*Pushes content to a *channel* created by the user, which others can subscribe to.
  @return Returns the pushbullet API response.
  @parameters
    channel_tag: The tag identifier for the channel to push to.
    params: The content to push. Can be a link, file, or any other pushable
      content, as long as it follows the PB API.*/
  def push(channel_tag: String, params: Map[String, String]) = {
    val channel = Map("channel_tag" -> channel_tag)
    val pushContent = params ++ channel
    val res = Http(pushAPI << pushContent OK as.String)

    res()
  }

}
