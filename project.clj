(defproject one "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [com.badlogicgames.gdx/gdx "1.9.2"]
                 [com.badlogicgames.gdx/gdx-platform "1.9.2" :classifier "natives-desktop"]
                 [com.badlogicgames.gdx/gdx-backend-lwjgl "1.9.2"]
                 [brute "0.4.0"]]
  :main ^:skip-aot one.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
