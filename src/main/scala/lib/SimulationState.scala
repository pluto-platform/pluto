package lib

trait SimulationState[T] {
  def setState(state: T): Unit

  def getState: T
}
