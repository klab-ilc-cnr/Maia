package it.cnr.ilc.maia.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.cnr.ilc.maia.dto.lexo.AssociateReferralEntryRequest;
import it.cnr.ilc.maia.dto.lexo.DictionaryAssociateEntryRequest;
import it.cnr.ilc.maia.dto.lexo.DictionaryCreateAndAssociateReferralEntryRequest;
import it.cnr.ilc.maia.dto.lexo.DictionaryCreateEntryRequest;
import it.cnr.ilc.maia.dto.lexo.DictionaryEntries;
import it.cnr.ilc.maia.dto.lexo.DictionaryEntriesRequest;
import it.cnr.ilc.maia.dto.lexo.DictionaryEntriesResponse;
import it.cnr.ilc.maia.dto.lexo.DictionaryEntryCoreResponse;
import it.cnr.ilc.maia.dto.lexo.DictionaryEntryCore;
import it.cnr.ilc.maia.dto.lexo.UpdateDictionaryEntryLabelRequest;
import it.cnr.ilc.maia.dto.lexo.GenericEntity;
import it.cnr.ilc.maia.dto.lexo.LexiconCreateEntryRequest;
import it.cnr.ilc.maia.dto.lexo.LexicalEntry;
import it.cnr.ilc.maia.dto.lexo.LexicographicComponent;
import it.cnr.ilc.maia.dto.lexo.LexicographicComponentResponse;
import it.cnr.ilc.maia.dto.lexo.LexicographicTree;
import it.cnr.ilc.maia.dto.lexo.LexiconCreateAndAssociateEntryRequest;
import it.cnr.ilc.maia.dto.lexo.MorphologyEntry;
import it.cnr.ilc.maia.dto.lexo.MorphologyEntryResponse;
import static it.cnr.ilc.maia.dto.lexo.PosTraitUtil.getLabel;
import it.cnr.ilc.maia.dto.lexo.PosTraitsResponse;
import it.cnr.ilc.maia.dto.lexo.UpdateDictionaryEntryStatusRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.DefaultUriBuilderFactory;

@RestController
@RequestMapping("/lexo")
public class LexoController extends ExternController {

    private RestTemplate textoRestTemplate;

    protected RestTemplate textoRestTemplate() throws Exception {
        if (textoRestTemplate == null) {
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((String hostname, SSLSession session) -> true);
            textoRestTemplate = new RestTemplate();
            String baseUrl = TextoController.class.getAnnotation(RequestMapping.class).value()[0].substring(1);
            baseUrl = environment.getProperty(baseUrl + ".url");
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory(baseUrl);
            textoRestTemplate.setUriTemplateHandler(uriBuilderFactory);
        }
        return textoRestTemplate;
    }

    @GetMapping("data/dictionaryEntry")
    public DictionaryEntryCoreResponse dataDictionaryEntry(@RequestParam(required = true) String id) throws Exception {
        DictionaryEntryCore dictionaryEntry = lexoGetDictionaryEntry(id);
        return new DictionaryEntryCoreResponse(dictionaryEntry);
    }

    @PostMapping("dictionary/create/entry")
    public DictionaryEntryCoreResponse dictionaryCreateEntry(@RequestParam String author, @RequestParam(required = true) String prefix, @RequestParam(required = true) String baseIRI, @RequestBody DictionaryCreateEntryRequest request) throws Exception {
        DictionaryEntryCore dictionaryEntry = lexoCreateDictionaryEntry(author, prefix, baseIRI);
        lexoUpdateDictionaryEntry(author, dictionaryEntry.getDictionaryEntry(), "http://www.w3.org/ns/lemon/lexicog#entry", request.getLang());
        lexoUpdateDictionaryEntry(author, dictionaryEntry.getDictionaryEntry(), "http://www.w3.org/2000/01/rdf-schema#label", request.getLabel());
        dictionaryEntry = lexoGetDictionaryEntry(dictionaryEntry.getDictionaryEntry());
        return new DictionaryEntryCoreResponse(dictionaryEntry);
    }

    private DictionaryEntryCore lexoCreateDictionaryEntry(String author, String prefix, String baseIRI) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Accept", Arrays.asList("application/json"));
        headers.put("Authorization", Arrays.asList(httpServletRequest.getHeader("Authorization")));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = "/create/dictionaryEntry?author={author}&prefix={prefix}&baseIRI={baseIRI}";
        Map<String, String> params = Map.of("author", author, "prefix", prefix, "baseIRI", baseIRI);
        ResponseEntity<String> response = restTemplate().exchange(url, HttpMethod.GET, entity, String.class, params);
        return new ObjectMapper().readValue(response.getBody(), DictionaryEntryCore.class);
    }

    public static record UpdateEntry(String relation, Object value) {}

    private void lexoUpdateDictionaryEntry(String author, String id, String relation, String value) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Content-Type", Arrays.asList("application/json"));
        headers.put("Authorization", Arrays.asList(httpServletRequest.getHeader("Authorization")));
        HttpEntity<UpdateEntry> entity = new HttpEntity<>(new UpdateEntry(relation, value), headers);
        String url = "/update/dictionaryEntry?author={author}&id={id}";
        Map<String, String> params = Map.of("author", author, "id", id);
        restTemplate().exchange(url, HttpMethod.POST, entity, String.class, params);
    }

    private DictionaryEntryCore lexoGetDictionaryEntry(String id) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Accept", Arrays.asList("application/json"));
        headers.put("Authorization", Arrays.asList(httpServletRequest.getHeader("Authorization")));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = "/data/dictionaryEntry?id={id}";
        Map<String, String> params = Map.of("id", id);
        ResponseEntity<String> response = restTemplate().exchange(url, HttpMethod.GET, entity, String.class, params);
        return new ObjectMapper().readValue(response.getBody(), DictionaryEntryCore.class);
    }

    @PostMapping("lexicon/create/entry")
    public LexicalEntry lexiconCreateEntry(@RequestParam String author, @RequestParam(required = true) String prefix, @RequestParam(required = true) String baseIRI, @RequestBody LexiconCreateEntryRequest request) throws Exception {
        LexicalEntry lexicalEntry = lexoCreateLexicalEntry(author, prefix, baseIRI);
        lexoUpdateLexicalEntry(author, lexicalEntry.getLexicalEntry(), "http://www.w3.org/ns/lemon/lime#entry", request.getLang());
        lexoUpdateLexicalEntry(author, lexicalEntry.getLexicalEntry(), "http://www.w3.org/2000/01/rdf-schema#label", request.getLabel());
        lexoUpdateLexicalEntry(author, lexicalEntry.getLexicalEntry(), "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", request.getType());
        lexoUpdateLinguisticRelation(lexicalEntry.getLexicalEntry(), "morphology", "http://www.lexinfo.net/ontology/3.0/lexinfo#partOfSpeech", request.getPos());
        return lexoGetLexicalEntry(lexicalEntry.getLexicalEntry());
    }

    private LexicalEntry lexoCreateLexicalEntry(String author, String prefix, String baseIRI) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Accept", Arrays.asList("application/json"));
        headers.put("Authorization", Arrays.asList(httpServletRequest.getHeader("Authorization")));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = "/create/lexicalEntry?author={author}&prefix={prefix}&baseIRI={baseIRI}";
        Map<String, String> params = Map.of("author", author, "prefix", prefix, "baseIRI", baseIRI);
        ResponseEntity<String> response = restTemplate().exchange(url, HttpMethod.GET, entity, String.class, params);
        return new ObjectMapper().readValue(response.getBody(), LexicalEntry.class);
    }

    private void lexoUpdateLexicalEntry(String author, String id, String relation, Object value) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Content-Type", Arrays.asList("application/json"));
        headers.put("Authorization", Arrays.asList(httpServletRequest.getHeader("Authorization")));
        HttpEntity<UpdateEntry> entity = new HttpEntity<>(new UpdateEntry(relation, value), headers);
        String url = "/update/lexicalEntry?author={author}&id={id}";
        Map<String, String> params = Map.of("author", author, "id", id);
        restTemplate().exchange(url, HttpMethod.POST, entity, String.class, params);
    }

    public static record UpdateRelation(String type, String relation, Object value) {}

    private void lexoUpdateLinguisticRelation(String id, String type, String relation, Object value) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Content-Type", Arrays.asList("application/json"));
        headers.put("Authorization", Arrays.asList(httpServletRequest.getHeader("Authorization")));
        HttpEntity<UpdateRelation> entity = new HttpEntity<>(new UpdateRelation(type, relation, value), headers);
        String url = "/update/linguisticRelation?id={id}";
        Map<String, String> params = Map.of("id", id);
        restTemplate().exchange(url, HttpMethod.POST, entity, String.class, params);
    }

    private LexicalEntry lexoGetLexicalEntry(String id) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Accept", Arrays.asList("application/json"));
        headers.put("Authorization", Arrays.asList(httpServletRequest.getHeader("Authorization")));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = "/data/lexicalEntry?id={id}&module={module}";
        Map<String, String> params = Map.of("id", id, "module", "core");
        ResponseEntity<String> response = restTemplate().exchange(url, HttpMethod.GET, entity, String.class, params);
        return new ObjectMapper().readValue(response.getBody(), LexicalEntry.class);
    }

    @PostMapping("dictionary/associate/entry")
    public Date dictionaryAssociateEntry(@RequestParam String author, @RequestParam(required = true) String prefix, @RequestParam(required = true) String baseIRI, @RequestBody DictionaryAssociateEntryRequest request) throws Exception {
//        LexicographicComponent lexicographicComponent = lexoCreateLexicographicComponent(author, prefix, baseIRI);
//        lexoUpdateLexicographicComponentPosition(request.getDictionaryEntryId(), "lexicog", "http://www.w3.org/1999/02/22-rdf-syntax-ns#_n", lexicographicComponent.getComponent(), request.getPosition());
//        lexoUpdateLinguisticRelation(lexicographicComponent.getComponent(), "lexicog", "http://www.w3.org/ns/lemon/lexicog#describes", request.getLexicalEntryId());
        lexoCreateLexicographicAssociation(author, request.getDictionaryEntryId(), request.getLexicalEntryId(), prefix, baseIRI, request.getPosition());
        return new Date();
    }

    private LexicographicComponent lexoCreateLexicographicComponent(String author, String prefix, String baseIRI) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Accept", Arrays.asList("application/json"));
        headers.put("Authorization", Arrays.asList(httpServletRequest.getHeader("Authorization")));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = "/create/lexicographicComponent?author={author}&prefix={prefix}&baseIRI={baseIRI}";
        Map<String, String> params = Map.of("author", author, "prefix", prefix, "baseIRI", baseIRI);
        ResponseEntity<String> response = restTemplate().exchange(url, HttpMethod.GET, entity, String.class, params);
        return new ObjectMapper().readValue(response.getBody(), LexicographicComponent.class);
    }

    public static record UpdateLexicographicComponent(String type, String relation, String component, Integer position) {}

    private void lexoUpdateLexicographicComponentPosition(String id, String type, String relation, String component, Integer position) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Content-Type", Arrays.asList("application/json"));
        headers.put("Authorization", Arrays.asList(httpServletRequest.getHeader("Authorization")));
        HttpEntity<UpdateLexicographicComponent> entity = new HttpEntity<>(new UpdateLexicographicComponent(type, relation, component, position), headers);
        String url = "/update/lexicographicComponentPosition?id={id}";
        Map<String, String> params = Map.of("id", id);
        restTemplate().exchange(url, HttpMethod.POST, entity, String.class, params);
    }

    @PostMapping("dictionary/createAndAssociate/entry")
    public Date dictionaryCreateAssociateEntry(@RequestParam String author, @RequestParam(required = true) String prefix, @RequestParam(required = true) String baseIRI, @RequestBody LexiconCreateAndAssociateEntryRequest request) throws Exception {
        LexicalEntry lexicalEntry = lexoCreateLexicalEntry(author, prefix, baseIRI);
        lexoUpdateLexicalEntry(author, lexicalEntry.getLexicalEntry(), "http://www.w3.org/ns/lemon/lime#entry", request.getLang());
        lexoUpdateLexicalEntry(author, lexicalEntry.getLexicalEntry(), "http://www.w3.org/2000/01/rdf-schema#label", request.getLabel());
        lexoUpdateLexicalEntry(author, lexicalEntry.getLexicalEntry(), "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", request.getType());
        lexoUpdateLinguisticRelation(lexicalEntry.getLexicalEntry(), "morphology", "http://www.lexinfo.net/ontology/3.0/lexinfo#partOfSpeech", request.getPos());
        LexicographicComponent lexicographicComponent = lexoCreateLexicographicComponent(author, prefix, baseIRI);
        lexoUpdateLinguisticRelation(lexicographicComponent.getComponent(), "lexicog", "http://www.w3.org/ns/lemon/lexicog#describes", lexicalEntry.getLexicalEntry());
        lexoUpdateLexicographicComponentPosition(request.getDictionaryEntryId(), "lexicog", "http://www.w3.org/1999/02/22-rdf-syntax-ns#_n", lexicographicComponent.getComponent(), request.getPosition());
        return new Date();
    }

    private void lexoCreateLexicographicAssociation(String author, String dictionaryEntryID, String lexicalEntryID, String prefix, String baseIRI, Integer position) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Accept", Arrays.asList("application/json"));
        headers.put("Authorization", Arrays.asList(httpServletRequest.getHeader("Authorization")));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = "/create/lexicographicAssociation?author={author}&dictionaryEntryID={dictionaryEntryID}&lexicalEntryID={lexicalEntryID}&prefix={prefix}&baseIRI={baseIRI}&position={position}";
        Map<String, String> params = Map.of(
                "author", author,
                "dictionaryEntryID", dictionaryEntryID,
                "lexicalEntryID", lexicalEntryID,
                "prefix", prefix,
                "baseIRI", baseIRI,
                "position", "" + position);
        restTemplate().exchange(url, HttpMethod.GET, entity, String.class, params);
    }

    @GetMapping("data/lexicographicComponents")
    public List<LexicographicComponentResponse> dataLexicographicComponents(@RequestParam(required = true) String id) throws Exception {
        List<LexicographicComponent> components = lexoLexicographicComponents(id);
        return components.stream().map(c -> new LexicographicComponentResponse(c)).collect(Collectors.toList());
    }

    private List<LexicographicComponent> lexoLexicographicComponents(String id) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Accept", Arrays.asList("application/json"));
        headers.put("Authorization", Arrays.asList(httpServletRequest.getHeader("Authorization")));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = "/data/lexicographicComponents?id={id}";
        Map<String, String> params = Map.of("id", id);
        ResponseEntity<String> response = restTemplate().exchange(url, HttpMethod.GET, entity, String.class, params);
        return new ObjectMapper().readValue(response.getBody(), new TypeReference<List<LexicographicComponent>>() {
        });
    }

    @PostMapping("data/dictionaryEntries")
    public DictionaryEntriesResponse dataDictionaryEntries(@RequestBody DictionaryEntriesRequest request) throws Exception {
        DictionaryEntries dictionaryEntries = lexoDataDictionaryEntries(request);
        return new DictionaryEntriesResponse(dictionaryEntries);
    }

    private DictionaryEntries lexoDataDictionaryEntries(DictionaryEntriesRequest request) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Accept", Arrays.asList("application/json"));
        headers.put("Authorization", Arrays.asList(httpServletRequest.getHeader("Authorization")));
        HttpEntity<DictionaryEntriesRequest> entity = new HttpEntity<>(request, headers);
        String url = "/data/dictionaryEntries";
        Map<String, String> params = Map.of();
        ResponseEntity<String> response = restTemplate().exchange(url, HttpMethod.POST, entity, String.class, params);
        return new ObjectMapper().readValue(response.getBody(), DictionaryEntries.class);
    }

    @PostMapping("dictionary/associate/referralEntry")
    public Date dataDictionaryEntries(@RequestBody AssociateReferralEntryRequest request) throws Exception {
        lexoUpdateGenericRelation(request.getReferralEntryId(), "reference", "http://www.w3.org/2002/07/owl#sameAs", request.getFullEntryId());
        return new Date();
    }

    private void lexoUpdateGenericRelation(String id, String type, String relation, Object value) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Content-Type", Arrays.asList("application/json"));
        headers.put("Authorization", Arrays.asList(httpServletRequest.getHeader("Authorization")));
        HttpEntity<UpdateRelation> entity = new HttpEntity<>(new UpdateRelation(type, relation, value), headers);
        String url = "/update/genericRelation?id={id}";
        Map<String, String> params = Map.of("id", id);
        restTemplate().exchange(url, HttpMethod.POST, entity, String.class, params);
    }

    @PostMapping("dictionary/createAndAssociate/referralEntry")
    public Date dictionaryCreateAndAssociateReferralEntry(@RequestParam String author, @RequestParam(required = true) String prefix, @RequestParam(required = true) String baseIRI, @RequestBody DictionaryCreateAndAssociateReferralEntryRequest request) throws Exception {
        DictionaryEntryCore dictionaryFullEntry = lexoCreateDictionaryEntry(author, prefix, baseIRI);
        lexoUpdateDictionaryEntry(author, dictionaryFullEntry.getDictionaryEntry(), "http://www.w3.org/ns/lemon/lexicog#entry", request.getLang());
        lexoUpdateDictionaryEntry(author, dictionaryFullEntry.getDictionaryEntry(), "http://www.w3.org/2000/01/rdf-schema#label", request.getFullEntryLabel());
        DictionaryEntryCore dictionaryReferralEntry = lexoCreateDictionaryEntry(author, prefix, baseIRI);
        lexoUpdateDictionaryEntry(author, dictionaryReferralEntry.getDictionaryEntry(), "http://www.w3.org/ns/lemon/lexicog#entry", request.getLang());
        lexoUpdateDictionaryEntry(author, dictionaryReferralEntry.getDictionaryEntry(), "http://www.w3.org/2000/01/rdf-schema#label", request.getReferralEntryLabel());
        lexoUpdateGenericRelation(dictionaryReferralEntry.getDictionaryEntry(), "reference", "http://www.w3.org/2002/07/owl#sameAs", dictionaryFullEntry.getDictionaryEntry());
        return new Date();
    }

    @GetMapping("data/dictionaryEntryByLexicalEntry")
    public List<String> dataDictionaryByLexicalEntry(@RequestParam(required = true) String id) throws Exception {
        List<DictionaryEntryCore> dictionaryEntryCores = lexoGetDictionaryEntryByLexicalEntry(id);
        return dictionaryEntryCores.stream().map(d -> d.getLabel()).collect(Collectors.toList());
    }

    @GetMapping("data/dictionaryEntryByLexicalEntry2")
    public DictionaryEntryCore dataDictionaryByLexicalEntry2(@RequestParam(required = true) String id) throws Exception {
        List<DictionaryEntryCore> dictionaryEntryCores = lexoGetDictionaryEntryByLexicalEntry(id);
        return dictionaryEntryCores.isEmpty() ? null : dictionaryEntryCores.get(0);
    }

    private List<DictionaryEntryCore> lexoGetDictionaryEntryByLexicalEntry(String id) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Accept", Arrays.asList("application/json"));
        headers.put("Authorization", Arrays.asList(httpServletRequest.getHeader("Authorization")));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = "/data/dictionaryEntryByLexicalEntry?id={id}";
        Map<String, String> params = Map.of("id", id);
        ResponseEntity<String> response = restTemplate().exchange(url, HttpMethod.GET, entity, String.class, params);
        return new ObjectMapper().readValue(response.getBody(), new TypeReference<List<DictionaryEntryCore>>() {
        });
    }

    @PostMapping("associate/lexicalConcept")
    public Date associateLexicalConcept(@RequestBody AssociateLexicalConceptRequest request) throws Exception {
        lexoUpdateLinguisticRelation(request.senseId, "conceptRel", "http://www.w3.org/ns/lemon/ontolex#isLexicalizedSenseOf", request.conceptId);
        return new Date();
    }

    public static record AssociateLexicalConceptRequest(String senseId, String conceptId) {}

    @PostMapping("dissociate/lexicalConcept")
    public Date dissociateLexicalConcept(@RequestBody AssociateLexicalConceptRequest request) throws Exception {
        lexoDeleteRelation(request.senseId, "http://www.w3.org/ns/lemon/ontolex#isLexicalizedSenseOf", request.conceptId);
        return new Date();
    }

    private void lexoDeleteRelation(String id, String relation, Object value) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Content-Type", Arrays.asList("application/json"));
        headers.put("Authorization", Arrays.asList(httpServletRequest.getHeader("Authorization")));
        HttpEntity<DeleteRelation> entity = new HttpEntity<>(new DeleteRelation(relation, value), headers);
        String url = "/delete/relation?id={id}";
        Map<String, String> params = Map.of("id", id);
        restTemplate().exchange(url, HttpMethod.POST, entity, String.class, params);
    }

    public static record DeleteRelation(String relation, Object value) {}

    @GetMapping("data/sense/lexicalConcepts")
    public List<GenericEntity> dataSenseLexicalConcepts(@RequestParam(required = true) String id) throws Exception {
        return lexoGetLinguisticRelations(id, "http://www.w3.org/ns/lemon/ontolex#isLexicalizedSenseOf");
    }

    private List<GenericEntity> lexoGetLinguisticRelations(String id, String property) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Accept", Arrays.asList("application/json"));
        headers.put("Authorization", Arrays.asList(httpServletRequest.getHeader("Authorization")));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = "/data/linguisticRelation?id={id}&property={property}";
        Map<String, String> params = Map.of("id", id, "property", property);
        ResponseEntity<String> response = restTemplate().exchange(url, HttpMethod.GET, entity, String.class, params);
        return new ObjectMapper().readValue(response.getBody(), new TypeReference<List<GenericEntity>>() {
        });
    }

    @GetMapping("data/dictionaryEntry/seeAlso")
    public List<GenericEntity> dataDictionaryEntrySeeAlso(@RequestParam(required = true) String id) throws Exception {
        return lexoGetGenericRelations(id, "http://www.w3.org/2000/01/rdf-schema#seeAlso");
    }

    private List<GenericEntity> lexoGetGenericRelations(String id, String property) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Accept", Arrays.asList("application/json"));
        headers.put("Authorization", Arrays.asList(httpServletRequest.getHeader("Authorization")));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = "/data/genericRelation?id={id}&property={property}";
        Map<String, String> params = Map.of("id", id, "property", property);
        ResponseEntity<String> response = restTemplate().exchange(url, HttpMethod.GET, entity, String.class, params);
        return new ObjectMapper().readValue(response.getBody(), new TypeReference<List<GenericEntity>>() {
        });
    }

    @PostMapping("update/dictionaryEntry/notes")
    public Date updatDictionaryEntryNotes(@RequestParam(required = true) String id, @RequestParam String author, @RequestBody String notes) throws Exception {
        notes = notes.replaceAll("\"", "\\\\\\\\\"").replaceAll("\\s", " ");
        lexoUpdateDictionaryEntry(author, id, "http://www.w3.org/2004/02/skos/core#note", notes);
        return new Date();
    }

    @GetMapping("data/lexicalConcepts")
    public Map dataLexicalConcepts(@RequestParam(required = true) String type) throws Exception {
        String id;
        if ("marcheDUso".equalsIgnoreCase(type)) {
            id = "http://lexica/mylexicon#cs_marca_d_uso";
        } else if ("marcheSemantiche".equalsIgnoreCase(type)) {
            id = "http://lexica/mylexicon#cs_marca_semantica";
        } else if ("marcheGrammaticali".equalsIgnoreCase(type)) {
            id = "http://lexica/mylexicon#cs_marca_grammaticale";
        } else {
            throw new Exception("unknown type");
        }
        return lexoDataLexicalConcepts(id);
    }

    private Map lexoDataLexicalConcepts(String id) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Accept", Arrays.asList("application/json"));
        headers.put("Authorization", Arrays.asList(httpServletRequest.getHeader("Authorization")));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = "/data/lexicalConcepts?id={id}";
        Map<String, String> params = Map.of("id", id);
        ResponseEntity<String> response = restTemplate().exchange(url, HttpMethod.GET, entity, String.class, params);
        return new ObjectMapper().readValue(response.getBody(), Map.class);
    }

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("dictionary/etymology/languages")
    public List<Etymology> dictionaryEtymologyLanguages() throws Exception {
        String sql = "select * from _etymology";
        return entityManager.createNativeQuery(sql, Etymology.class).getResultList();
    }

    public static record Etymology(String code, String name) {}

    @GetMapping("dictionary/otherDocuments")
    public List<OtherDocument> dictionaryOtherDocuments() throws Exception {
        String sql = "select code, title, `order` from _other_document order by `order`, code";
        return entityManager.createNativeQuery(sql, OtherDocument.class).getResultList();
    }

    public static record OtherDocument(String code, String title, Integer order) {}

    @GetMapping("dictionary/authorDocuments")
    public List<OtherDocument> dictionaryAuthorDocuments() throws Exception {
        String sql = "select code, title, `order` from _author_document order by `order`, code";
        return entityManager.createNativeQuery(sql, OtherDocument.class).getResultList();
    }

    @PostMapping("associate/dictionaryEntry/seeAlso")
    public Date associateDictionaryEntrySeeAlso(@RequestBody AssociateSeeAlsoRequest request) throws Exception {
        lexoUpdateGenericRelation(request.dictionaryId, "reference", "http://www.w3.org/2000/01/rdf-schema#seeAlso", request.seeAlsoDictionaryId);
        return new Date();
    }

    public static record AssociateSeeAlsoRequest(String dictionaryId, String seeAlsoDictionaryId) {}

    @PostMapping("delete/dictionaryEntry/seeAlso")
    public Date deleteDictionaryEntrySeeAlso(@RequestBody AssociateSeeAlsoRequest request) throws Exception {
        lexoDeleteRelation(request.dictionaryId, "http://www.w3.org/2000/01/rdf-schema#seeAlso", request.seeAlsoDictionaryId);
        return new Date();
    }

    @PostMapping("update/dictionaryEntry/label")
    public Date updateDictionaryEntryLabel(@RequestParam(required = true) String id, @RequestParam String author, @RequestBody UpdateDictionaryEntryLabelRequest label) throws Exception {
        lexoUpdateDictionaryEntry(author, id, "http://www.w3.org/2000/01/rdf-schema#label", label.getLabel());
        return new Date();
    }

    @PostMapping("update/dictionaryEntry/status")
    public Date updateDictionaryEntryStatus(@RequestParam(required = true) String id, @RequestParam String author, @RequestBody UpdateDictionaryEntryStatusRequest status) throws Exception {
        lexoUpdateDictionaryEntry(author, id, "http://www.w3.org/2003/06/sw-vocab-status/ns#term_status", status.getStatus());
        return new Date();
    }

    @GetMapping("dictionary/sortingTree")
    public List<LexicographicTree> dataLemmaSensesTree(@RequestParam(required = true) String id) throws Exception {
        List<LexicographicComponent> components = lexoLexicographicComponents(id);
        List<LexicographicTree> trees = new ArrayList<>(components.size());
        LexicographicTree tree;
        for (LexicographicComponent component : components) {
            tree = new LexicographicTree(component);
            trees.add(tree);
            tree.setChildren(dataLemmaSensesTree(tree.getReferredEntity()));
        }
        return trees;
    }

    @PostMapping("update/lemmaSenseTree")
    public Date updateLemmaSensesTree(@RequestParam String author, @RequestParam(required = true) String prefix, @RequestParam(required = true) String baseIRI, @RequestParam(required = true) String id, @RequestBody List<LexicographicTree> trees) throws Exception {
        for (LexicographicTree tree : trees) {
            lexoDeleteLexicographicComponent(tree.getId());
        }
        doUpdateLemmaSensesTree(author, prefix, baseIRI, id, trees);
        return new Date();
    }

    private void lexoDeleteLexicographicComponent(String id) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Content-Type", Arrays.asList("application/json"));
        headers.put("Authorization", Arrays.asList(httpServletRequest.getHeader("Authorization")));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = "/delete/lexicographicComponent?id={id}";
        Map<String, String> params = Map.of("id", id);
        restTemplate().exchange(url, HttpMethod.GET, entity, String.class, params);
    }

    public void doUpdateLemmaSensesTree(String author, String prefix, String baseIRI, String id, List<LexicographicTree> trees) throws Exception {
        DictionaryAssociateEntryRequest request;
        for (int i = 0; i < trees.size(); i++) {
            request = new DictionaryAssociateEntryRequest();
            request.setDictionaryEntryId(id);
            request.setLexicalEntryId(trees.get(i).getReferredEntity());
            request.setPosition(i + 1);
            LexicographicComponent lexicographicComponent = lexoCreateLexicographicComponent(author, prefix, baseIRI);
            lexoUpdateLexicographicComponentPosition(request.getDictionaryEntryId(), "lexicog", "http://www.w3.org/1999/02/22-rdf-syntax-ns#_n", lexicographicComponent.getComponent(), request.getPosition());
            lexoUpdateLinguisticRelationSensesTree(lexicographicComponent.getComponent(), "lexicog", "http://www.w3.org/ns/lemon/lexicog#describes", request.getLexicalEntryId());
            doUpdateLemmaSensesTree(author, prefix, baseIRI, trees.get(i).getReferredEntity(), trees.get(i).getChildren());
        }
    }

    public static record UpdateRelationSensesTree(String type, String relation, Object value, Boolean sensesCustomOrder) {}

    private void lexoUpdateLinguisticRelationSensesTree(String id, String type, String relation, Object value) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Content-Type", Arrays.asList("application/json"));
        headers.put("Authorization", Arrays.asList(httpServletRequest.getHeader("Authorization")));
        HttpEntity<UpdateRelationSensesTree> entity = new HttpEntity<>(new UpdateRelationSensesTree(type, relation, value, Boolean.TRUE), headers);
        String url = "/update/linguisticRelation?id={id}";
        Map<String, String> params = Map.of("id", id);
        restTemplate().exchange(url, HttpMethod.POST, entity, String.class, params);
    }

    @PostMapping("ontology/data/upload")
    public void upload(@RequestParam("file") MultipartFile file) throws Exception {
        HttpHeaders headers = new HttpHeaders(getHeaders(httpServletRequest));
        headers.remove("content-length");
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        UrlAndParams urlAndParams = getUrlAndPArams(httpServletRequest);
        restTemplate().exchange(urlAndParams.url, HttpMethod.POST, entity, Void.class, urlAndParams.params);
    }

    @GetMapping("data/dictionaryEntryPosTraits")
    public List<PosTraitsResponse> dataDictionaryEntryPosTraits(@RequestParam(required = true) String id) throws Exception {
        List list = new ArrayList();
        List<LexicographicComponent> components = lexoLexicographicComponents(id);
        for (LexicographicComponent component : components) {
            LexicalEntry lexicalEntry = lexoGetLexicalEntry(component.getReferredEntity());
            List<Map<String, String>> traits = lexicalEntry.getMorphology();
            list.add(new PosTraitsResponse(traits.stream().map(m -> Map.of("trait", getLabel(m.get("trait")), "value", getLabel(m.get("value")))).toList()));
        }
        return list;
    }

    @GetMapping("lexinfo/data/morphology")
    public List<MorphologyEntryResponse> lexinfoDataMorphology() throws Exception {
        boolean filter = environment.getProperty("lexo.morphology.pos-filter", Boolean.class);
        return lexoLexinfoDataMorphology().stream()
                .map(m -> new MorphologyEntryResponse(m, filter))
                .toList();
    }

    private List<MorphologyEntry> lexoLexinfoDataMorphology() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Accept", Arrays.asList("application/json"));
        headers.put("Authorization", Arrays.asList(httpServletRequest.getHeader("Authorization")));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = "/lexinfo/data/morphology";
        ResponseEntity<String> response = restTemplate().exchange(url, HttpMethod.GET, entity, String.class);
        return new ObjectMapper().readValue(response.getBody(), new TypeReference<List<MorphologyEntry>>() {
        });
    }

    @GetMapping("delete/form")
    public String deleteForm(@RequestParam(required = true) String id) throws Exception {
        if (textoExistsAnnotationFeatire(id).isEmpty()) {
            return lexoDelete(id, "form");
        } else {
            throw new Exception("Deletion not allowed: entity is referenced by existing annotations.");
        }
    }

    @GetMapping("delete/lexicalSense")
    public String deleteLexicalSense(@RequestParam(required = true) String id) throws Exception {
        if (textoExistsAnnotationFeatire(id).isEmpty()) {
            return lexoDelete(id, "lexicalSense");
        } else {
            throw new Exception("Deletion not allowed: entity is referenced by existing annotations.");
        }
    }

    @GetMapping("delete/lexicalEntry")
    public String deleteLexicalEntry(@RequestParam(required = true) String id) throws Exception {
        if (textoExistsAnnotationFeatire(id).isEmpty()) {
            return lexoDelete(id, "lexicalEntry");
        } else {
            throw new Exception("Deletion not allowed: entity is referenced by existing annotations.");
        }
    }

    private List<Map<String, Object>> textoExistsAnnotationFeatire(String value) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", Arrays.asList(httpServletRequest.getHeader("Authorization")));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = "/annotationFeature/list?where={where}";
        Map<String, String> params = Map.of("where", "value='" + value + "'");
        ResponseEntity<String> response = textoRestTemplate().exchange(url, HttpMethod.GET, entity, String.class, params);
        return new ObjectMapper().readValue(response.getBody(), new TypeReference<List<Map<String, Object>>>() {
        });
    }

    private String lexoDelete(String id, String topic) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", Arrays.asList(httpServletRequest.getHeader("Authorization")));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = "/delete/" + topic + "?id={id}";
        Map<String, String> params = Map.of("id", id);
        return restTemplate().exchange(url, HttpMethod.GET, entity, String.class, params).getBody();
    }
}
