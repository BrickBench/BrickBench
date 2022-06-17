(ns utils)

(import (com.opengg.loader Project BrickBench) (com.opengg.core.engine Resource OpenGG))

(def brickbench (com.opengg.loader.BrickBench/CURRENT))

(defn toRunnable [func]
  (reify Runnable (run [this] (func))))





