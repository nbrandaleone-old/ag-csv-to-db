(ns ag-csv-to-db.core
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

;;  This program converts a CSV file into a database format
;;  suitable for use in a Cryptzone AppGate server.
;;  We must parse the CSV input file into appropriate
;;  services, components and roles.
;;
;;  Nick Brandaleone - January 2016


; Command line options.
(def cli-options
  ;; An option with a required argument
  [["-s" "--source FILE" "File in CSV format"
    :default "/tmp/AG_database.csv"]
  ["-o" "--output FILE" "Output File"
   :default "/tmp/changes.db"]
  ["-h" "--help"]])

;; Files for testing. Fix: hard-coding output target-file. Bad...
(def output-file "/tmp/changes.db")

;; remove comments at beginning of file
(defn remove-comments [text]
  (rest
    (drop-while (fn [item] (not= "START" (first item))) text)))

;; Clojure only provides first and second
(def third #(nth % 2))
(def fourth #(nth % 3))

;; Extract the interesting part
(def extract-role (juxt second third fourth))

;; Append to a file using spit, adding a newline
(defn spitn [path text]
  (spit path (str text "\n") :append true))

;; Parse the role
;; FIX: I hard-coded the service. I should either search the rows for a service, or be explicit in the CSV data format.
(defn parse-role [role-data-row]
  (let [[name desc icon] (extract-role role-data-row)]
    (str "role=" name "; descr=" desc "; icon=" icon "; flags=shareable; qualifier=true; service=web_test always")))

;; Parse the service.
;; FIX: I hard-coded the components. I must make this dynamic.
(defn parse-service [service-data-row]
  (let [[type name desc icon] service-data-row]
       (str type "=" name "; descr=" desc "; icon=" icon
            "; start=host1_custom, host2_custom, host3_custom, host4_custom, host5_custom")))

;; Parse the components. For now only ipaccess
(defn parse-component [component-data-row]
  (let [[type name desc icon protocol mode lport dport host log] component-data-row]
    (str type "=" name "; descr=" desc "; proto=" protocol 
         "; locport=" lport "; mode=" mode "; desthosts=" host "; destports=" dport)))

;; Parse the data into various parts
(defn parse [text]
  (let [[type] text]; name desc icon protocol mode lport dport log
    (spitn output-file
      (cond
        (= "role" type)    (parse-role text)
        (= "service" type) (parse-service text)
        (= "ipaccess" type)(parse-component text)))))
        ; else clause

;; macro `with-open` ensures that the file is closed when we are done with it
;; The file is read lazily, so it is best to use doall to force read.
(defn read-csv [csv-file]
  (with-open [reader (io/reader csv-file)]
    (->> (csv/read-csv reader)
         (remove-comments)
         (map parse)
         (doall)))) ; Is doall necessary? Can I keep it `lazy`?

;; How to write a CSV file
; (with-open [out-file (io/writer "out.csv")]
;  (csv/write-csv out-file [["this" "is"]["a" "test"]]))
;; -> nil

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Command line arguments and main handling
(defn usage [options-summary]
  (->> ["This program converts a CSV file into native appgate database format."
        "There is no syntax checking done here, although that is done on the AG server."
        ""
        "Usage: ag_csv_to_db.java -s <input_file.csv> -o <output_file>"
        "The output file defaults to /tmp/change.db"
        ""
        "Options:"
        options-summary
        ""]
       (clojure.string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occured while parsing your arguments:\n\n"
       (clojure.string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    ; (println [options arguments])
    ;; Handle help and error conditions
    (cond
      (:help options) (exit 0 (usage summary))
;      ((not= (count arguments) 1) (exit 1 (usage summary))
      errors (exit 1 (error-msg errors)))
    ;; Execute program with options
    (read-csv (get-in options [:source]))))
;    (case (first arguments)
;      "start" (server/start! options)
;      "stop" (server/stop! options)
;      "status" (server/status! options)
;      (exit 1 (usage summary)))))

