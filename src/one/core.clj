(ns one.core
  (:gen-class)
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication LwjglApplicationConfiguration]
           [com.badlogic.gdx Gdx ApplicationListener Input Input$Keys]
           [com.badlogic.gdx.graphics GL20 Texture]
           [com.badlogic.gdx.graphics.g2d TextureRegion Animation SpriteBatch]
           [com.badlogic.gdx.files FileHandle]
           [com.badlogic.gdx.utils Array]
           [com.badlogic.gdx.scenes.scene2d Stage]
           [com.badlogic.gdx.scenes.scene2d.ui TextButton TextButton$TextButtonStyle])
  (:require [clojure.data.json :as json]
            [brute.entity :as entity]))

(defonce exception-lock (atom false))

(defonce walk-animation (atom ()))
(defonce walk-spritesheet (atom ()))
(defonce walk-frames (atom ()))
(defonce sprite-batch (atom ()))
(defonce current-frame (atom ()))

(defonce state-time (atom 0))

(defonce current-anim (atom 0))
(defonce anim-count (atom 0))

(defonce gui-stage (atom nil))

(def ecs (atom nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; components

(defrecord SpriteComponent [])
(defrecord SpriteAnimationComponent [fsm])
(defrecord PositionComponent [x y])
(defrecord PlayerControllerComponent [])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-animation [id meta frames]
  (if-let [anim-meta (->> (get-in meta ["meta" "frameTags"])
                          (filter #(= (get % "name") id))
                          first)]
    (let [start (get anim-meta "from")
          end (get anim-meta "to")
          anim-frames (subvec (vec frames) start (+ 1 end))]
      (Animation. 0.1 (Array. (into-array anim-frames))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-ui [stage]
  (let [button (doto (TextButton. "Click me" (TextButton$TextButtonStyle.))
                 (.setWidth 200)
                 (.setHeight 20)
                 (.setPosition (- (/ (.getWidth Gdx/graphics) 2) 100)
                               (- (/ (.getHeight Gdx/graphics) 2) 10)))]
    (.addActor stage button)
    (.setInputProcessor Gdx/input stage)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn init-ecs []
  (let [player (entity/create-entity)]
    (-> (entity/create-system)
        (entity/add-entity player)
        (entity/add-component player (->PositionComponent 50 50))
        (entity/add-component player (->SpriteComponent))
        (entity/add-component player (->PlayerControllerComponent)))))

(defn on-create [game]

  (reset! ecs (init-ecs))

  ;; setup ui

  (reset! gui-stage (Stage.))
  ;(create-ui @gui-stage)

  ;; read spritesheet metadata:

  (reset! walk-spritesheet (Texture. "resources/blockydude.png"))

  (let [file-reader (-> "blockydude.json" clojure.java.io/resource .getFile clojure.java.io/reader)
        metadata (json/read file-reader)
        tile-width (-> (get metadata "frames") first (get-in ["sourceSize" "w"]))
        tile-height (-> (get metadata "frames") first (get-in ["sourceSize" "h"]))
        anim (-> (get-in metadata "meta" "frameTags") first)]

    (reset! current-anim 0)

    (let [frames (TextureRegion/split @walk-spritesheet tile-width tile-height)
          frames (areduce frames i ret []
                          (concat ret (vec (aget frames i))))]
      (reset! walk-frames frames)
      (reset! walk-animation (get-animation "Walk" metadata frames)))
    (reset! sprite-batch (SpriteBatch.))
    (reset! state-time 0)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn reset [game]
  ;; TODO cleanup anything we need to
  (reset! exception-lock false)
  (on-create game))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn on-render [game]

  (.glClear Gdx/gl (bit-or GL20/GL_COLOR_BUFFER_BIT GL20/GL_DEPTH_BUFFER_BIT))

  (swap! state-time #(+ % (.getDeltaTime Gdx/graphics)))

  (.begin @sprite-batch)
  (.draw @sprite-batch (.getKeyFrame @walk-animation @state-time true) (float 50.0) (float 50.0))
  (.end @sprite-batch)

  (when (.isKeyPressed Gdx/input Input$Keys/R)
    (reset game))
  (when (.isKeyPressed Gdx/input Input$Keys/Q)
    (.exit Gdx/app)))

(defn exception-wrapper [game f]
  (if (not @exception-lock)
    (try (f game)
         (catch Exception e
           (.printStackTrace e)
           (reset! exception-lock true)))
    (do (when (.isKeyPressed Gdx/input Input$Keys/R)
          (reset game))
        (when (.isKeyPressed Gdx/input Input$Keys/Q)
          (.exit Gdx/app)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def app
  (reify ApplicationListener
    (create [this] (exception-wrapper this on-create))
    (dispose [this])
    (render [this] (exception-wrapper this on-render))
    (resize [this width height])
    (pause [this])
    (resume [this])))

(defn -main
  [& args]
  (let [config (doto (LwjglApplicationConfiguration.)
                 (-> .width (set! 800))
                 (-> .height (set! 600))
                 (-> .title (set! "ONE"))
                 (-> .forceExit (set! false)))]
    (println "Starting ONE...")
    (LwjglApplication. app config)))
