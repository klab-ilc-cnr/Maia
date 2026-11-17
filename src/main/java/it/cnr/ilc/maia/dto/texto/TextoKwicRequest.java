package it.cnr.ilc.maia.dto.texto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 *
 * @author oakgen
 */
@Getter
public class TextoKwicRequest {

    private final List<Long> resources;
    private final Long layer;
    private final Integer width;
    private final Boolean reload;
    private final String query;
    private final List<KwicFeatureFilter> features;
    private final String query2;
    private final List<KwicFeatureFilter> features2;

    public static record KwicFeatureFilter(Long feature, String[] values) {}

    public static record FeatureIds(Long semanticsFeatureId, Long posFeatureId, Long namedEntityFeatureId) {}

    public TextoKwicRequest(KwicRequest maiaRequest) {
        resources = maiaRequest.getResources();
        layer = maiaRequest.getLayerId();
        StringBuilder builder = new StringBuilder();
        String value = maiaRequest.getFilters().getSearchValue();
        value = value.replaceAll("'", "''");
        String operand = "=";
        if (value.matches(".*[*?].*")) {
            operand = "like";
            value = value.replaceAll("\\*", "%").replaceAll("\\?", "_");
        }
        if ("form".equals(maiaRequest.getFilters().getSearchMode())) {
            builder.append("token ").append(operand).append(" '").append(value).append("'");
        } else if ("lemma".equals(maiaRequest.getFilters().getSearchMode())) {
            builder.append("lemma ").append(operand).append(" '").append(value).append("'");
        } else {
            throw new RuntimeException("searchMode unknown");
        }
        if (maiaRequest.getFilters().getPos() != null && !maiaRequest.getFilters().getPos().isEmpty()) {
            builder.append("and pos in ")
                    .append(maiaRequest.getFilters().getPos().stream()
                            .map(p -> "'" + p + "'")
                            .collect(Collectors.joining(",", "(", ")")));
        }
        reload = maiaRequest.getReload();
        width = maiaRequest.getFilters().getContextLength();
        query = builder.toString();
        features = null;
        query2 = null;
        features2 = null;
    }

    public TextoKwicRequest(SearchRequest maiaRequest, FeatureIds featureIds) {
        resources = maiaRequest.getResources();
        layer = null;
        StringBuilder builder = new StringBuilder();
        String value = maiaRequest.getFilters().getSearchValue();
        value = value.replaceAll("'", "''");
        String operand = "=";
        if (value.matches(".*[*?].*")) {
            operand = "like";
            value = value.replaceAll("\\*", "%").replaceAll("\\?", "_");
        }
        if ("form".equals(maiaRequest.getFilters().getSearchMode())) {
            builder.append("token ").append(operand).append(" '").append(value).append("'");
        } else if ("lemma".equals(maiaRequest.getFilters().getSearchMode())) {
            builder.append("lemma ").append(operand).append(" '").append(value).append("'");
        } else {
            throw new RuntimeException("searchMode unknown");
        }
        reload = maiaRequest.getReload();
        width = maiaRequest.getFilters().getContextLength();
        query = builder.toString();
        features = new ArrayList<>();
        if (maiaRequest.getFilters().getSemantics() != null && maiaRequest.getFilters().getSemantics().length > 0) {
            features.add(new KwicFeatureFilter(featureIds.semanticsFeatureId, maiaRequest.getFilters().getSemantics()));
        }
        if (maiaRequest.getFilters().getPoss() != null && maiaRequest.getFilters().getPoss().length > 0) {
            features.add(new KwicFeatureFilter(featureIds.posFeatureId, maiaRequest.getFilters().getPoss()));
        }
        if (maiaRequest.getFilters().getNamedEntities() != null && maiaRequest.getFilters().getNamedEntities().length > 0) {
            features.add(new KwicFeatureFilter(featureIds.namedEntityFeatureId, maiaRequest.getFilters().getNamedEntities()));
        }
        if (maiaRequest.getFilters().getCooccurValue() != null) {
            if (maiaRequest.getFilters().getCooccurMode() == null) {
                throw new RuntimeException("co-occurrence mode missing");
            }
            builder = new StringBuilder();
            value = maiaRequest.getFilters().getCooccurValue();
            value = value.replaceAll("'", "''");
            operand = "=";
            if (value.matches(".*[*?].*")) {
                operand = "like";
                value = value.replaceAll("\\*", "%").replaceAll("\\?", "_");
            }
            if ("form".equals(maiaRequest.getFilters().getCooccurMode())) {
                builder.append("token ").append(operand).append(" '").append(value).append("'");
            } else if ("lemma".equals(maiaRequest.getFilters().getCooccurMode())) {
                builder.append("lemma ").append(operand).append(" '").append(value).append("'");
            } else {
                throw new RuntimeException("searchMode unknown");
            }
            query2 = builder.toString();
        } else {
            query2 = null;
        }
        List<KwicFeatureFilter> temp = new ArrayList<>();
        if (maiaRequest.getFilters().getCooccurSemantics() != null && maiaRequest.getFilters().getCooccurSemantics().length > 0) {
            temp.add(new KwicFeatureFilter(featureIds.semanticsFeatureId, maiaRequest.getFilters().getCooccurSemantics()));
        }
        if (maiaRequest.getFilters().getCooccurPoss() != null && maiaRequest.getFilters().getCooccurPoss().length > 0) {
            temp.add(new KwicFeatureFilter(featureIds.posFeatureId, maiaRequest.getFilters().getCooccurPoss()));
        }
        if (maiaRequest.getFilters().getCooccurNamedEntities() != null && maiaRequest.getFilters().getCooccurNamedEntities().length > 0) {
            temp.add(new KwicFeatureFilter(featureIds.namedEntityFeatureId, maiaRequest.getFilters().getCooccurNamedEntities()));
        }
        features2 = temp.isEmpty() ? null : temp;
    }

}
