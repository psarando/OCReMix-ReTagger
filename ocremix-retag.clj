(use '[leiningen.exec :only (deps)]
     '[leiningen.core.project :only (defproject)])

(deps '[[me.raynes/fs "1.4.4"]
        [org/jaudiotagger "2.0.3"]
        [org.clojure/tools.cli "0.3.1"]
        [org.clojure/clojure "1.5.1"]])

(require '[clojure.tools.cli :as cli]
         '[me.raynes.fs :as fs])

(import '[java.util.logging Logger Level]
        '[org.jaudiotagger.audio AudioFileIO AudioFileFilter]
        '[org.jaudiotagger.tag.id3 ID3v23FieldKey]
        '[org.jaudiotagger.tag.id3.framebody FrameBodyTIT3])

(defn verify-mp3-file
  [file-path]
  (let [file (fs/file file-path)
        file-filter (AudioFileFilter.)]
    (and (fs/file? file) (.accept file-filter file))))

(defn print-frame
  [id value]
    (println (str id ":") value))

(defn print-txt-frame
  [id tag-name txt-frame]
    (print-frame (str id "(" tag-name ")") (.getContent txt-frame)))

(defn print-multi-line-frame
  [id tag-name txt]
    (print-frame (str id "(" tag-name ")") (str "\n" txt)))

(defn print-tag-field
  [tag tag-field]
  (let [id (.getId tag-field)]
    (case id
      "TIT1" (print-txt-frame id "Grouping" tag-field)
      "TIT2" (print-txt-frame id "Title" tag-field)
      "TIT3" (print-txt-frame id "SubTitle" tag-field)
      "TPE1" (print-txt-frame id "Artist" tag-field)
      "TPE2" (print-txt-frame id "AlbumArtist" tag-field)
      "TALB" (print-txt-frame id "Album" tag-field)
      "TSOA" (print-txt-frame id "SortAlbum" tag-field)
      "TRCK" (print-txt-frame id "Track" tag-field)
      "TOPE" (print-txt-frame id "OriginalArtist" tag-field)
      "TOAL" (print-txt-frame id "OriginalAlbum" tag-field)
      "TCOM" (print-txt-frame id "Composer" tag-field)
      "TYER" (print-txt-frame id "Year" tag-field)
      "TCON" (print-txt-frame id "Genre" tag-field)
      "TENC" (print-txt-frame id "Encoder" tag-field)
      "TPUB" (print-txt-frame id "Publisher" tag-field)
      "TCOP" (print-txt-frame id "Copyright" tag-field)
      "TXXX" (print-txt-frame id "UserText" tag-field)
      "WXXX" (print-txt-frame id "Url" tag-field)
      "WOAR" (print-txt-frame id "ArtistUrl" tag-field)
      "TCMP" (print-txt-frame id "Compilaton" tag-field)
      "COMM" (print-multi-line-frame id "Comment" (.getContent tag-field))
      "USLT" (print-multi-line-frame id "Lyrics" (.getLyric (.getBody tag-field)))
      (print-frame (str id "(" (.size (.getFields tag id)) ")") tag-field))))

(defn parse-file
  [file-path]
  (println "\nfile:" file-path)
  (when (verify-mp3-file file-path)
    (let [mp3file (AudioFileIO/read (fs/file file-path))
          tag (.getID3v2Tag mp3file)
          fields (.getFields tag)]
      (dorun
       (while (.hasNext fields)
         (print-tag-field tag (.next fields)))))))

(defn set-txt-field
  [tag key value]
  (.setField tag (.createField tag key value)))

(defn swap-titles
  [tag]
  (let [title (.getFirst tag ID3v23FieldKey/TITLE)
        subtitle-field (.getFirstField tag "TIT3")
        subtitle-body (.getBody subtitle-field)
        subtitle (.getContent subtitle-field)
        swap-title (> (.length title) (.length subtitle))]
    (when swap-title
      (set-txt-field tag ID3v23FieldKey/TITLE subtitle)
      (.setBody subtitle-field (FrameBodyTIT3. (.getTextEncoding subtitle-body) title)))
    swap-title))

(defn swap-album
  [tag]
  (let [album (.getFirst tag ID3v23FieldKey/ALBUM)
        grouping (.getFirst tag ID3v23FieldKey/GROUPING)
        ocremix-album (re-find #"(https?://)?(ocremix.org)" album)]
    (when ocremix-album
      (set-txt-field tag ID3v23FieldKey/ALBUM grouping)
      (set-txt-field tag ID3v23FieldKey/GROUPING album))
    ocremix-album))

(defn set-album-sort
  [tag sort-album]
  (set-txt-field tag ID3v23FieldKey/ALBUM_SORT sort-album)
  true)

(defn add-album-sort
  [tag]
  (let [album (.getFirst tag ID3v23FieldKey/ALBUM)
        sort-album (.getFirst tag ID3v23FieldKey/ALBUM_SORT)]
    (when (and (not (.hasField tag "TSOA")) (.startsWith album "Final Fantasy"))
      (case album
        "Final Fantasy II" (set-album-sort tag "Final Fantasy 02")
        "Final Fantasy III" (set-album-sort tag "Final Fantasy 03")
        "Final Fantasy IV" (set-album-sort tag "Final Fantasy 04")
        "Final Fantasy V" (set-album-sort tag "Final Fantasy 05")
        "Final Fantasy VI" (set-album-sort tag "Final Fantasy 06")
        "Final Fantasy VII" (set-album-sort tag "Final Fantasy 07")
        "Final Fantasy VIII" (set-album-sort tag "Final Fantasy 08")
        "Final Fantasy IX" (set-album-sort tag "Final Fantasy 09")
        "Final Fantasy X" (set-album-sort tag "Final Fantasy 10")
        "Final Fantasy X-2" (set-album-sort tag "Final Fantasy 10-2")
        "Final Fantasy XI Online" (set-album-sort tag "Final Fantasy 11")
        "Final Fantasy XII" (set-album-sort tag "Final Fantasy 12")
        "Final Fantasy XIII" (set-album-sort tag "Final Fantasy 13")
        "Final Fantasy XIV" (set-album-sort tag "Final Fantasy 14")
        false))))

(defn translate-composer
  "Respect"
  [tag]
  (let [composer (.getFirst tag ID3v23FieldKey/COMPOSER)
        translate (.contains composer "Nobuo Uematsu")]
    (when translate
      (set-txt-field
       tag ID3v23FieldKey/COMPOSER
       (clojure.string/replace composer #"Nobuo Uematsu" "Nobuo Uematsu (植松伸夫)")))
    translate))

(defn update-tags
  [update-dir file-path]
  (when (verify-mp3-file file-path)
    (let [file (fs/file file-path)
          new-file (fs/file update-dir (fs/base-name file-path))
          mp3file (AudioFileIO/read file)
          tag (.getID3v2Tag mp3file)
          fields (.getFields tag)
          updated-titles (swap-titles tag)
          updated-album (swap-album tag)
          updated-album-sort (add-album-sort tag)
          updated-composer (translate-composer tag)]
      (when (or updated-titles
                updated-album
                updated-album-sort
                updated-composer)
        (println "Writing new tags to" (.getPath new-file))
        (.save mp3file (fs/copy file new-file))))))

(defn process-file
  [parse? update-dir file-path]
  (let [directory (fs/parent file-path)]
    (fs/with-cwd
     directory
     (if parse?
       (parse-file file-path)
       (do
        (when-not (fs/exists? update-dir) (fs/mkdir update-dir))
         (update-tags update-dir file-path))))))

(defn process-directory
  [parse? update-dir directory]
  (fs/with-cwd
   directory
   (if parse?
     (dorun (map parse-file (fs/list-dir ".")))
     (do
      (when-not (fs/exists? update-dir) (fs/mkdir update-dir))
      (dorun (map (partial update-tags update-dir)
           (fs/list-dir ".")))))))

(defn parse-args
  [args]
  (cli/cli
   args
   ["-h" "--help" "Show help." :default false :flag true]
   ["-p" "--parse" "Parse Tags." :default false :flag true]
   ["-r" "--retagdir" "Destination directory for retagged files (by default, a subdirectory created along side the processed files)." :default "retagged"]
   ["-f" "--files" "Process Files." :default false :flag true]
   ["-d" "--directory" "Process Directories." :default false :flag true]))

(defn validate-opts
  [opts help-str]
  (let [f (:files opts)
        d (:directory opts)]
    (when (or (and f d)
              (not (or f d)))
    (do (println "Please specify one of --files (-f) or --directory (-d).")
        (println help-str)
        (System/exit 1)))))

(defn main-func
  []
  (let [args *command-line-args*
        [opts paths help-str] (parse-args args)
        paths (drop 1 paths)
        update-dir (:retagdir opts)]
    (when (:help opts)
      (do (println help-str)
          (System/exit 0)))
    (validate-opts opts help-str)
    (doto (Logger/getLogger "org.jaudiotagger")
      (.setLevel Level/WARNING))
    (when (:files opts)
      (dorun (map (partial process-file (:parse opts) update-dir) paths)))
    (when (:directory opts)
      (dorun (map (partial process-directory (:parse opts) update-dir) paths)))))

(main-func)
