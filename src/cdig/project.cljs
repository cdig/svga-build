(ns cdig.project
  (:require
   [cdig.fs :as fs]
   [cdig.io :as io]))

(declare pull)

(def system-files [".gitignore" "cdig.json" "package.json"])
(def generated-files [".DS_Store" ".git" "bower.json" "bower_components" "deploy" "gulpfile.coffee" "node_modules" "package-lock.json" "public" "yarn.lock" "yarn-error.log"])
(def new-project-files {:cd-module ["source/index.kit" "source/pages/objectives.html" "source/styles/fonts.scss"]
                        :svga ["source/symbol.coffee" "source/config.coffee"]})

(defn- pull-from-origin [type files]
  (let [base-url (str "https://raw.githubusercontent.com/cdig/" (name type) "-starter/v4/dist/")]
    (dorun (map #(fs/download (str base-url %) %) files))))

(defn- gulp [cmd]
  (io/exec "gulp --gulpfile node_modules/cd-core/gulpfile.coffee --cwd ." cmd))

(defn project-name []
  (fs/current-dirname))

(defn index-name []
  (if (fs/dir? "deploy")
    (fs/basename (first (fs/readdir "deploy/index")))))

(defn clean []
  (fs/rm generated-files)
  (fs/rm system-files))

(defn compile [type]
  (gulp (str (name type) ":prod")))

(defn new-project [type]
  (let [type-files (get new-project-files type)]
    (fs/mkdir "resources")
    (pull-from-origin type type-files)
    (pull type)))

(defn pull [type fast]
  (fs/rm system-files)
  (pull-from-origin type system-files)
  (cond
    (fs/path-exists? (str (fs/homedir) "/cdig/cli/node_modules")) ; Copy node_modules from ~
    (io/exec "cp -a ~/cdig/cli/node_modules .")
    
    (or fast ; Do a fast install
        (not (fs/path-exists? "yarn.lock")))
    (io/exec "yarn install")
    
    ; Do a slow install
    :else (io/exec "yarn upgrade")))

(defn push []
  (let [project (project-name)
        index (index-name)
        era "v4"
        s3-path (str "s3://lbs-cdn/" era "/")]
    (io/exec "aws s3 sync deploy/all" s3-path "--size-only --exclude \".*\" --cache-control max-age=31536000,immutable")))

(defn watch [type]
  (if (fs/path-exists? "node_modules")
    (gulp (str (name type) ":dev"))
    (io/print :red "Looks like you forgot to pull.")))
