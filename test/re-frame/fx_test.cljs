(ns re-frame.fx-test
  (:require
    [cljs.test :refer-macros [is deftest async use-fixtures]]
    [re-frame.core :as re-frame]
    [re-frame.fx]
    [re-frame.interop :refer [set-timeout!]]))

;; ---- FIXTURES ---------------------------------------------------------------

;; This fixture uses the re-frame.core/make-restore-fn to checkpoint and reset
;; to cleanup any dynamically registered handlers from our tests.
(defn fixture-re-frame
  []
  (let [restore-re-frame (atom nil)]
    {:before #(reset! restore-re-frame (re-frame.core/make-restore-fn))
     :after  #(@restore-re-frame)}))

(use-fixtures :each (fixture-re-frame))

;; ---- TESTS ------------------------------------------------------------------

(deftest dispatch-later
  (let [seen-events (atom [])]
    ;; Setup and excercise effects handler with :dispatch-later.
    (re-frame/reg-event-fx
      ::later-test
      (fn [_world _event-v]
        (re-frame/reg-event-db
          ::watcher
          (fn [db [_ token]]
            (is (#{:event1 :event2 :event3} token) "unexpected: token passed through")
            (swap! seen-events #(conj % token))
            db))
        {:dispatch-later [{:ms 100 :dispatch [::watcher :event1]}
                          {:ms 200 :dispatch [::watcher :event2]}
                          {:ms 200 :dispatch [::watcher :event3]}]}))

    (async done
      (set-timeout!
        (fn []
          (is (= @seen-events [:event1 :event2 :event3]) "All 3 events should have fired in order")
          (done))
        1000)
      ;; kick off main handler
      (re-frame/dispatch [::later-test]))))

