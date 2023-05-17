package kr.lavalse.kweekview.exception

import kr.lavalse.kweekview.model.WeekEvent
import java.time.LocalDateTime

open class WeekViewException(message: String) : Exception("[WeekView] $message")

class CantTouchAllDayException: WeekViewException("종일 일정은 선택할 수 없습니다.")
class TimeIsReversedException: WeekViewException("시작시간이 끝 시간보다 큼.")
class AlreadyExistException(event: WeekEvent): WeekViewException("이미 등록되어 있는 스케쥴입니다. (ID: ${event.id})")
class NotExistException: WeekViewException("해당되는 스케쥴이 없음")
class IsPastFromTodayException(today: LocalDateTime, value: LocalDateTime): WeekViewException("기준일($today) 보다 이전 ($value)")