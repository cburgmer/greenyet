(ns greenyet.core)

(defn handler [x]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "<body>Don't worry, be happy</body>"})
