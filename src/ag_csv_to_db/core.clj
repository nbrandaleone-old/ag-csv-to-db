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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Global defs
(def output-file "/tmp/changes.db")
(def component-list (atom [])) ; keep track of all the components
(def service-list (atom []))   ; keeps track of services

;; remove comments at beginning of file
(defn remove-comments [text]
  (rest
    (drop-while (fn [item] (not= "START" (first item))) text)))

;; Clojure only provides first and second
(def third #(nth % 2))
(def fourth #(nth % 3))

;; Extract the interesting part
(def extract-role (juxt second third fourth))
(def extract-service ())
(def extract-component ())

;; Append to a file using spit, adding a newline
(defn spitn [path text]
  (spit path (str text "\n") :append true))

;; Parse the role
(defn parse-role [role-data-row]
  (println (str "The service list is: " @service-list))
  (let [[name desc icon] (extract-role role-data-row)
        services @service-list]
    (reset! service-list [])
    (str "role=" name "; descr=" desc "; icon=" icon 
         "; flags=shareable; qualifier=true; service="
         (if (= 1 (count services)) (str (first services) " always")
           (apply str (interpose ", always" services))))))

;; Parse the service.
(defn parse-service [service-data-row]
  (println (str "The component list is: " @component-list))
  (let [[type name desc icon] service-data-row
        components @component-list]
    (reset! component-list [])
    (swap! service-list conj name)
    (str type "=" name "; descr=" desc "; icon=" icon
            "; start=" (apply str (interpose "," components)))))

; "host1_custom, host2_custom, host3_custom, host4_custom, host5_custom")))

;; Parse the components. For now only ipaccess
(defn parse-component [component-data-row]
  (let [[type name desc icon protocol mode lport dport host log] 
        component-data-row] ; we use all but icon
    (swap! component-list conj name)
    (str type "=" name "; descr=" desc "; proto=" protocol 
         "; locport=" lport "; mode=" mode "; desthosts=" 
         host "; destports=" dport "; loglevel=" log)))

;;  text - represents a line of text from the CSV source file
(defn parse [text]
"Parse the data into various parts and write new format to file"
  (let [[type] text] 
    ; type name desc icon protocol mode lport dport dhost log
    (spitn output-file
      (cond
        (= "role" type)    (parse-role text) 
        (= "service" type) (parse-service text)
        (= "ipaccess" type)(parse-component text)))))
        ; else clause

;; macro `with-open` ensures that the file is closed when we are done with it
;; The file is read lazily, so it is best to use doall to force read.
(defn read-csv [csv-file]
  "reads the input file, one line at a time,
  strips the comments at the beginning of the file, and parses the rest"
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

; Command line options.
(def cli-options
  ;; An option with a required argument
  [["-s" "--source FILE" "File in CSV format"
    :default "/tmp/AG_database.csv"]
  ["-o" "--output FILE" "Output File"
   :default "/tmp/changes.db"]
  ["-h" "--help"]])

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

