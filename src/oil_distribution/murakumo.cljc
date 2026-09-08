(ns oil_distribution.murakumo
  "Pure cljc actor boundary generated from manifest migration scaffold."
  (:require [kotoba.lang.text :as str]))

(def actor-did
  "did:web:oil-distribution.etzhayyim.com")

(def common-gates
  [:council-charter-attestation
   :no-platform-held-key-baseline
   :no-probing-baseline
   :murakumo-only-inference-baseline
   :did-primary-baseline
   :append-only-gate-baseline
   :kotoba-only-substrate-baseline])

(defn collection
  [name]
  (str "com.etzhayyim.oil-distribution." name))

(def cell-specs {
  :getproductterminal {:legacy-cell "com-etzhayyim-apps-oilDistribution-network-getProductTerminal"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "getproductterminal")]
     :required-gates common-gates
     :trigger "manifest cell getproductterminal"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :listproductterminals {:legacy-cell "com-etzhayyim-apps-oilDistribution-network-listProductTerminals"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "listproductterminals")]
     :required-gates common-gates
     :trigger "manifest cell listproductterminals"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :listwholesalehubs {:legacy-cell "com-etzhayyim-apps-oilDistribution-network-listWholesaleHubs"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "listwholesalehubs")]
     :required-gates common-gates
     :trigger "manifest cell listwholesalehubs"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :getproductbalance {:legacy-cell "com-etzhayyim-apps-oilDistribution-analytics-getProductBalance"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "getproductbalance")]
     :required-gates common-gates
     :trigger "manifest cell getproductbalance"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :health {:legacy-cell "com-etzhayyim-apps-oilDistribution-health"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "health")]
     :required-gates common-gates
     :trigger "manifest cell health"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :shinkaevolution {:legacy-cell "com-etzhayyim-apps-standard-shinkaEvolution"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "shinkaevolution")]
     :required-gates common-gates
     :trigger "manifest cell shinkaevolution"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :shinkaknowledge {:legacy-cell "com-etzhayyim-apps-standard-shinkaKnowledge"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "shinkaknowledge")]
     :required-gates common-gates
     :trigger "manifest cell shinkaknowledge"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :shinka {:legacy-cell "shinka"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "shinka")]
     :required-gates common-gates
     :trigger "manifest cell shinka"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :koji {:legacy-cell "koji"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "koji")]
     :required-gates common-gates
     :trigger "manifest cell koji"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :kyumei {:legacy-cell "kyumei"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "kyumei")]
     :required-gates common-gates
     :trigger "manifest cell kyumei"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :domain-knowledge {:legacy-cell "domain-knowledge"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "domain-knowledge")]
     :required-gates common-gates
     :trigger "manifest cell domain-knowledge"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :productterminal {:legacy-cell "com-etzhayyim-apps-oilDistribution-productTerminal"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "productterminal")]
     :required-gates common-gates
     :trigger "manifest cell productterminal"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :wholesalehub {:legacy-cell "com-etzhayyim-apps-oilDistribution-wholesaleHub"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "wholesalehub")]
     :required-gates common-gates
     :trigger "manifest cell wholesalehub"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :yieldsnapshot {:legacy-cell "com-etzhayyim-apps-oilRefining-yieldSnapshot"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "yieldsnapshot")]
     :required-gates common-gates
     :trigger "manifest cell yieldsnapshot"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
  :cargo {:legacy-cell "com-etzhayyim-apps-oilShipping-cargo"
     :phase :event
     :murakumo-node "reuben"
     :collections [(collection "cargo")]
     :required-gates common-gates
     :trigger "manifest cell cargo"
     :ceiling "Manifest-driven migration scaffold; explicit execution stays in runtime methods"}
})

(defn safe-rkey
  [s]
  (let [clean (-> (str s)
                  (str/replace #"^did:web:" "")
                  (str/replace #"[^A-Za-z0-9._~-]" "-"))]
    (if (str/blank? clean) "unknown" clean)))

(defn gate-value
  [attestations gate]
  (or (get attestations gate)
      (get attestations (name gate))
      (when (set? attestations) (attestations gate))
      (when (set? attestations) (attestations (name gate)))))

(defn missing-gates
  [spec attestations]
  (->> (:required-gates spec)
       (remove #(boolean (gate-value attestations %)))
       vec))

(defn put-record-effect
  [collection rkey record]
  {:op :mst/put-record
   :actor actor-did
   :collection collection
   :rkey rkey
   :record record})

(defn records-for
  [spec {:keys [records record computed-at request-id]
         :as input}]
  (let [input-records (cond
                        (map? records) records
                        (some? record) {0 record}
                        :else {})
        base {:actorDid actor-did
              :computedAt computed-at
              :legacyCell (:legacy-cell spec)
              :phase (:phase spec)
              :requestId request-id
              :actorBoundary "cljc-migration-scaffold"
              :scaffold true
              :constitutionalStatus "attested-plan"}]
    (map-indexed
     (fn [idx coll]
       (let [record* (merge {:$type coll}
                            base
                            (or (get input-records coll)
                                (get input-records idx)
                                {}))
             rkey (safe-rkey (or (:rkey record*)
                                 (get record* "rkey")
                                 (:tid record*)
                                 request-id
                                 (str (:legacy-cell spec) "-" idx)))]
         {:collection coll
          :record record*
          :rkey rkey}))
     (:collections spec))))

(defn cell-plan
  [cell-key {:keys [attestations] :as input}]
  (let [spec (get cell-specs cell-key)]
    (when-not spec
      (throw (ex-info "unknown cell" {:cell cell-key})))
    (let [missing (missing-gates spec attestations)]
      (merge
       {:cell cell-key
        :legacy-cell (:legacy-cell spec)
        :actor actor-did
        :phase (:phase spec)
        :murakumo-node (:murakumo-node spec)
        :trigger (:trigger spec)
        :ceiling (:ceiling spec)
        :required-gates (:required-gates spec)
        :missing-gates missing}
       (if (seq missing)
         {:status :blocked
          :effects []}
         (let [planned-records (records-for spec input)]
           {:status :ready
            :records (vec planned-records)
            :effects (mapv (fn [{:keys [collection record rkey]}]
                             (put-record-effect collection rkey record))
                           planned-records)}))))))

(defn all-cell-plans
  [input]
  (into {}
        (map (fn [cell-key] [cell-key (cell-plan cell-key input)]))
        (keys cell-specs)))
