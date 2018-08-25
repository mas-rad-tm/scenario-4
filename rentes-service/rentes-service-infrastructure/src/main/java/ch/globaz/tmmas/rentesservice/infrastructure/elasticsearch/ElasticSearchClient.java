package ch.globaz.tmmas.rentesservice.infrastructure.elasticsearch;

import ch.globaz.tmmas.rentesservice.domain.model.droit.index.DossierIndex;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Client elasticsearch réactif "adapté". <br/>
 * - les repository de Spring Data Elasticsearch ne supportent pas les appels réactifs (non bloquants)<br/>
 * - L'API > 6 d'ElasticSearch supporte les appels non-bloquants mais sous forme de callback<br/>
 * - Afin d'éviter de se retrouver dans une callback hell, ce client va encapsuler les callback dans de flux réactif (Mono ou Flux)<br/>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ElasticSearchClient {

    @Autowired
    private final RestHighLevelClient client;
    @Autowired
    private final ObjectMapper objectMapper;

    private static final String DOSSIER_INDEX = "dossiers";

    /**
     * Recherche un document pas son username (id es)
     * @param userName le nom d'utilisateur recherché
     * @return une instance de Mono contenant potentiellement l'élément
     */
    public ResponseEntity findByUserName(String userName) throws IOException {

        GetResponse getResponse = client.get(new GetRequest(DOSSIER_INDEX, DOSSIER_INDEX, userName));

        if(getResponse.isExists()){
            return new ResponseEntity<DossierIndex>(objectMapper.convertValue(getResponse.getSource(),DossierIndex.class),
                    HttpStatus.OK);
        }
         return ResponseEntity.notFound().build();
    }

    public List<DossierIndex> recherche(String methode,String terme) throws IOException {

        log.debug("Search with methode : {} and terme: {}",methode,terme);

        switch (methode){
            case "fuzzy":
                log.debug("fuzzy search");
                return fuzzy(fuzzySearchRequest(terme, DOSSIER_INDEX));
            case "wildcards":
                log.debug("wildcard search");
                return wildCard(wildCardsSearchRequest(terme, DOSSIER_INDEX));
            case "composed":
                log.debug("composed search");
                return composed(composedSearchRequest(terme, DOSSIER_INDEX));
            default:
                throw new IllegalArgumentException("methode name specified error : " + methode);
        }

    }

    private List<DossierIndex> composed(MultiSearchRequest searchRequest) throws IOException {

        MultiSearchResponse composerResponse = client.multiSearch(searchRequest);

        MultiSearchResponse.Item first = composerResponse.getResponses()[0];
        log.info("First multisearch response {}",first.getResponse().getHits().totalHits);

        MultiSearchResponse.Item second = composerResponse.getResponses()[1];
        log.info("Second multisearch response {}",second.getResponse().getHits().totalHits);

        Stream<SearchHit> firstStream = Stream.of(first.getResponse().getHits().getHits());
        Stream<SearchHit> secondStream = Stream.of(second.getResponse().getHits().getHits());


        List<DossierIndex> personnes = Stream.concat(firstStream,secondStream)
                .map(hit -> {
                    log.info("Sync multisearch composed flux iteration for serialzation:; {}",hit.getSourceAsString());
                    try {
                        return objectMapper.readValue(hit.getSourceAsString(), DossierIndex.class);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                }).collect(Collectors.toList());



            return personnes
                    .stream()
                    .distinct()
                    .collect(Collectors.toList());

    }


    private List<DossierIndex> wildCard(SearchRequest searchRequest) throws IOException {


        SearchResponse response = client.search(searchRequest);

        Stream<SearchHit> fluxWildCards = Stream.of(response.getHits().getHits());

        return  fluxWildCards.map(hit -> {
            log.info(hit.getSourceAsString());
            try {
                return objectMapper.readValue(hit.getSourceAsString(),DossierIndex.class);
            } catch (IOException e) {
                log.error("IO Exception when deserialising hit :" + hit.getSourceAsString());
                return null;
            }
        }).collect(Collectors.toList());



    }

    public List<DossierIndex> fuzzy(SearchRequest searchRequest) throws IOException {


        SearchResponse response = client.search(searchRequest);

        Stream<SearchHit> fluxWildCards = Stream.of(response.getHits().getHits());

        return  fluxWildCards.map(hit -> {
            log.info(hit.getSourceAsString());
            try {
                return objectMapper.readValue(hit.getSourceAsString(),DossierIndex.class);
            } catch (IOException e) {
                log.error("IO Exception when deserialising hit :" + hit.getSourceAsString());
                return null;
            }
        }).collect(Collectors.toList());

    }

    /**
     * Indexe un élément de type {@code Personne}
     * @param doc le document à indéxer
     * @return une Mono contenant potentiellement la réponse
     */
    public IndexResponse index(DossierIndex doc) throws IOException {
        return indexDoc(doc);
    }

    /**
     * Indexe un élément de type {@code Personne}
     * @param docs le document à indéxer
     * @return une Mono contenant potentiellement la réponse
     */
    public BulkResponse bulkIndex(List<DossierIndex> docs) throws IOException {
        return bulkIndexDocs(docs);
    }


    private IndexResponse indexDoc(DossierIndex doc) throws IOException {
        return doIndex(doc);

    }
    private BulkResponse bulkIndexDocs(List<DossierIndex> docs) throws IOException {
         return doBulkIndex(docs);

    }


    private IndexResponse doIndex(DossierIndex doc) throws IOException {
        final IndexRequest indexRequest = new IndexRequest(DOSSIER_INDEX, DOSSIER_INDEX, doc.getId().toString());
        final String json = objectMapper.writeValueAsString(doc);
        indexRequest.source(json, XContentType.JSON);
        return client.index(indexRequest);
    }

    private BulkResponse doBulkIndex(List<DossierIndex> docs) throws
            IOException {
        BulkRequest bulkRequest = new BulkRequest();

        docs.forEach(doc -> {

            try {

                IndexRequest indexRequest = new IndexRequest(DOSSIER_INDEX, DOSSIER_INDEX, doc.getId().toString());
                String json = objectMapper.writeValueAsString(doc);
                indexRequest.source(json, XContentType.JSON);
                bulkRequest.add(indexRequest);

            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });

        return client.bulk(bulkRequest,null);
    }



    private SearchRequest wildCardsSearchRequest(String terme, String index) {

        QueryBuilder wildCardQueryBuilder = QueryBuilderFactory.wildCarsdQueryBuilder(terme);

        return new SearchRequest()
                .indices(index)
                .source(new SearchSourceBuilder().query(wildCardQueryBuilder).from(0).size(5));

    }

    private MultiSearchRequest composedSearchRequest(String terme, String index) {

        MultiSearchRequest multiSearchRequest  = new MultiSearchRequest();
        multiSearchRequest.add(wildCardsSearchRequest(terme,index));
        multiSearchRequest.add(fuzzySearchRequest(terme,index));

        return multiSearchRequest;
    }


    private SearchRequest fuzzySearchRequest(String terme, String index){

        QueryBuilder fuzzyCardQueryBuilder = QueryBuilderFactory.fuzzyQueryBuilder(terme);

        return new SearchRequest()
                .indices(index)
                .source(new SearchSourceBuilder().query(fuzzyCardQueryBuilder).from(0).size(5));


    }


}
