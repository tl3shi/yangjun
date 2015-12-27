package controllers

import java.io.File
import java.io.File
import java.util.zip.{ZipEntry, ZipOutputStream}
import com.google.common.io.Files
import com.typesafe.scalalogging.slf4j.Logging
import org.joda.time.DateTime
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.Files
import play.api.libs.json._
import play.api.mvc._
import play.utils.UriEncoding
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

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
        JsonResult("wrong", s"$fileNameIn 不存在")
    }
  }




   def createZip(sourceFiles: List[File], zipPath: String) {
      var fos: FileOutputStream = null;
      var zos: ZipOutputStream = null;
      try {
        fos = new FileOutputStream(zipPath);
        zos = new ZipOutputStream(fos);
        sourceFiles.foreach(sourcePath => {
          writeZip(sourcePath, "", zos);
        })
      } catch  {
        case e: Throwable => logger.error("创建ZIP文件失败",e);
      } finally {
        try {
          if (zos != null) {
            zos.close();
          }
        } catch {
          case e: Throwable => logger.error("创建ZIP文件失败",e);
        }

      }
    }

    private def writeZip(file: File, parentPath: String, zos: ZipOutputStream) {
      if(file.exists()){
        var fis: FileInputStream = null;
        var  dis: DataInputStream = null;
        try {
          fis = new FileInputStream(file);
          dis = new DataInputStream(new BufferedInputStream(fis));
          val ze: ZipEntry = new ZipEntry(parentPath + file.getName());
          zos.putNextEntry(ze);
          val content = new Array[Byte](1024)
          var len: Int = 0;
          while(len != -1) {
            len = fis.read(content)
            zos.write(content, 0, len);
            zos.flush();
          }
        } catch {
          case e: Throwable => logger.error("创建ZIP文件失败",e);
        } finally {
          try {
            if(dis != null){
              dis.close();
            }
          } catch {
            case e: Throwable => logger.error("创建ZIP文件失败",e);
          }
        }
      }
  }

  import java.io.BufferedReader;
  import java.io.InputStreamReader;

  def exeCmd(commandStr: String): String = {
      var br: BufferedReader = null;
      try {
        val p: Process = Runtime.getRuntime().exec(commandStr);
        br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        var line: String = null;
        val sb: StringBuilder = new StringBuilder();
        while ( line != null) {
          line = br.readLine()
          sb.append(line + "\n");
        }
        sb.toString()
      } catch  {
        case e: Throwable => logger.error("Error", e);
      } finally {
        if (br != null) {
          try {
            br.close();
          } catch  {
            case e: Throwable => logger.error(" br.close()",e);
          }
        }
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
              Some(new File("PFX2PEM.pem"))
            }
            case "PFX2KDB" => {
              // TODO4
              None
            }
            case "PFX2JKS" => {
              // TODO
              Some(new File("PFX2JKS.jks"))
            }
            case "PFX2P7B" => {
              // TODO
              None
            }
          }
        })
        logger.info(s"transform result: $results")
        val fileResult = s"${tmp}/result.zip" // 结果文件
        createZip(results.toList, fileResult)
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
