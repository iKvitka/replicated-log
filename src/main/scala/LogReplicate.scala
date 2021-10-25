import play.api.libs.json.{Format, Json}

case class LogReplicate(data: String, writeConcern: Int)

object LogReplicate {
  implicit val format: Format[LogReplicate] = Json.format[LogReplicate]
}
