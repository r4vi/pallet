(ns pallet.resource.remote-file
  "File Contents."
  (:use pallet.script
        pallet.stevedore
        [pallet.utils :only [cmd-join register-file-transfer!]]
        [pallet.resource :only [defcomponent]]
        [pallet.resource.file :only [adjust-file heredoc]]
        clojure.contrib.logging))

(defn remote-file*
  [path & options]
  (let [opts (merge {:action :create} (apply hash-map options))]
    (condp = (opts :action)
      :create
      (let [url (opts :url)
            content (opts :content)
            md5 (opts :md5)
            local-file (opts :local-file)]
        (cmd-join
          [(cond
             (and url md5) (script
                             (if-not (file-exists? ~path)
                               (do (if-not
                                       (== ~md5 @(md5sum ~path "|" cut "-f1 -d' '"))
                                     (wget "-O" ~path ~url))))
                             (echo "MD5 sum is" @(md5sum ~path)))
             url (script
                   (wget "-O" ~path ~url)
                   (echo "MD5 sum is" @(md5sum ~path)))
             content (apply heredoc
                       path content
                       (apply concat (seq (select-keys opts [:literal]))))
             local-file (let [temp-path (register-file-transfer! local-file)]
                          (script (mv ~temp-path ~path)))
             :else "")
          (adjust-file path opts)])))))

(defcomponent remote-file "File contents management."
  remote-file* [filename & options])