package com.yuiwai.marimo.field

import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.yuiwai.marimo.field.api.{FieldId, FieldObject, FieldService}

import scala.concurrent.{ExecutionContext, Future}

class FieldServiceImpl(implicit ec: ExecutionContext) extends FieldService {
  override def getObjects: ServiceCall[FieldId, Seq[FieldObject]] = ServiceCall { fieldId: FieldId =>
    Future(Seq(FieldObject(1), FieldObject(2)))
  }
}
