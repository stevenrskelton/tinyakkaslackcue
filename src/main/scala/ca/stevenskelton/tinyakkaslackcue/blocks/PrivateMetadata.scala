package ca.stevenskelton.tinyakkaslackcue.blocks

case class PrivateMetadata private(value: String) extends AnyVal {
  def block:String = s""""private_metadata": "$value""""
}

object PrivateMetadata {
  val Empty = PrivateMetadata("")
//  def apply(value: String): PrivateMetadata = new PrivateMetadata(value.take(25))
}