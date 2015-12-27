package controllers

import java.io.File
import java.io.File
import com.google.common.io.Files
import com.typesafe.scalalogging.slf4j.Logging
import org.joda.time.DateTime
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.Files
import play.api.libs.json._
import play.api.mvc._
import play.utils.UriEncoding

import scala.collection.immutable.Set


object Application extends Controller with Logging {

  def index = Action {
    Ok(views.html.index())
  }

  lazy val config = play.Play.application().configuration()
  val fileMaxSize = 5 * 1024 * 1024

  //val allowedExtensions = Set("pfx", "chain")
  val tmp = {
    val dir = config.getString("upload", "tmp")
    val f = new File(dir)
    if (!f.exists())
      f.mkdirs()
    dir
  }


  def exportFile(fileNameIn: String) = Action { implicit request =>
    val filename = UriEncoding.decodePath(fileNameIn, "UTF-8")
    logger.info(s"fileRequest: ${filename}")
    val file = new java.io.File(filename)
    if (file.exists())
      Ok.sendFile(file)
    else{
      val file2 = new java.io.File("/" + filename) //the browser may eat the filepath starts with "/"
      if (file2.exists())
        Ok.sendFile(file2)
      else
        JsonResult("wrong", "wrong")
    }
  }

  def transform() = Action { implicit request =>
    actionForm.bindFromRequest.fold({
      formWithError => JsonResult("error", "validate error!")
    }, {
      formWithSuccess => {
        val params = formWithSuccess._1
        val pfx = formWithSuccess._2
        val chain = formWithSuccess._3
        logger.info(s"transform: $formWithSuccess")
        val results = params.split(",").flatMap(action => {
          action match {
            case "PFX2PEM" => {
              // TODO
              Some(new File("PFX2PEM.result"))
            }
            case "PFX2KDB" => {
              // TODO
              None
            }
            case "PFX2JKS" => {
              // TODO
              None
            }
            case "PFX2P7B" => {
              // TODO
              None
            }
          }
        })
        logger.info(s"transform result: $results")
        val fileResult = s"${tmp}/result.zip" // 结果文件 TODO
        val data = Json.obj("url" -> fileResult)
        JsonResult("ok", "ok", data)
      }
    })
  }

  def upload(fileType: String) = Action { implicit request =>
    try {
      val body = request.body.asMultipartFormData.get
      val newFile = body.file(s"file-$fileType").map({ file =>
        if (file.ref.file.length() > fileMaxSize) throw new IllegalArgumentException(s"file too large, max is ${fileMaxSize}")
        // check ext
        val ext = file.filename.substring(file.filename.lastIndexOf(".")+1)
        if (ext != fileType) throw new Exception(s"禁止上传${file.filename}")
        // save to file
        val filename = s"${DateTime.now.getMillis}.$ext"
        val f = new File(s"${tmp}/${filename}")
        file.ref.moveTo(f)
        logger.info(s"file: $f")
        f
      })
      val filePath = newFile.get.getAbsolutePath
      Ok(Json.obj("status" -> "ok", "url" -> JsString(filePath)))
    } catch {
      case ex: Exception => {
        logger.warn(s"upload exception: ${ex}")
        Ok(Json.obj("status" -> "failed", "msg" -> ex.toString))
      }
    }
  }

  def JsonResult(status: String, msg: String, data: JsValue = JsNull) = {
    Ok(Json.obj("status" -> status, "msg" -> msg, "data" -> data))
  }

  val actionForm = Form(tuple(
    "action" -> nonEmptyText,
    "pfx" -> nonEmptyText,
    "chain" -> nonEmptyText
  ))

}
