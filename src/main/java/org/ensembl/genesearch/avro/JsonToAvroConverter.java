package org.ensembl.genesearch.avro;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.Path;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import parquet.avro.AvroParquetWriter;

public class JsonToAvroConverter {

	public static <T> Stream<T> asStream(Iterator<T> sourceIterator) {
		return asStream(sourceIterator, false);
	}

	public static <T> Stream<T> asStream(Iterator<T> sourceIterator,
			boolean parallel) {
		Iterable<T> iterable = () -> sourceIterator;
		return StreamSupport.stream(iterable.spliterator(), parallel);
	}

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	protected void addString(JsonNode node, String elem, Object target) {
		try {
			log.trace("Trying to add string " + elem);
			JsonNode sNode = node.get(elem);
			if (sNode != null)
				target.getClass()
						.getDeclaredMethod(nameToSetter(elem),
								CharSequence.class)
						.invoke(target, sNode.asText());
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	protected void addInt(JsonNode node, String elem, Object target) {
		try {
			log.trace("Trying to add int " + elem);

			JsonNode iNode = node.get(elem);
			if (iNode != null)
				target.getClass()
						.getDeclaredMethod(nameToSetter(elem), Integer.class)
						.invoke(target, iNode.asInt());
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	protected void addStrings(JsonNode node, String elem, Object target) {
		try {
			log.trace("Trying to add strings " + elem);
			JsonNode accElem = node.get(elem);
			List<String> accs = null;
			if (accElem != null) {
				log.debug("Adding " + elem);
				accs = asStream(accElem.getElements()).map(j -> j.asText())
						.collect(Collectors.toList());
			} else {
				accs = Collections.EMPTY_LIST;
			}
			target.getClass().getDeclaredMethod(nameToSetter(elem), List.class)
					.invoke(target, accs);

		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	protected class TypeAwarePredicate implements Predicate<Field> {

		private final Type type;

		public TypeAwarePredicate(Type type) {
			this.type = type;
		}

		@Override
		public boolean test(Field t) {
			if (t.schema().getType() != null) {
				return t.schema().getType().equals(type);
			} else {
				return t.schema().getTypes().stream()
						.anyMatch(t2 -> type.equals(t2.getType()));
			}
		}

	}

	protected final TypeAwarePredicate stringPredicate = new TypeAwarePredicate(
			Type.STRING);
	protected final TypeAwarePredicate intPredicate = new TypeAwarePredicate(
			Type.INT);
	protected final TypeAwarePredicate arrayPredicate = new TypeAwarePredicate(
			Type.ARRAY);

	protected void setAllSimpleValues(JsonNode node, List<Field> fields,
			Object target) {
		fields.stream().filter(stringPredicate)
				.forEach(field -> addString(node, field.name(), target));
		fields.stream().filter(intPredicate)
				.forEach(field -> addInt(node, field.name(), target));
		fields.stream()
				.filter(arrayPredicate)
				.filter(field -> field.schema().getElementType().getType()
						.equals(Type.STRING))
				.forEach(field -> addStrings(node, field.name(), target));
	}

	protected static String nameToSetter(String name) {
		StringBuilder setter = new StringBuilder("set");
		int start = 0;
		int i = 0;
		while ((i = name.indexOf('_', start)) != -1) {
			setter.append(StringUtils.capitalize(name.substring(start, i)));
			start = i + 1;
		}
		setter.append(StringUtils.capitalize(name.substring(start)));
		return setter.toString();
	}

	public List<Gene> parseGenes(JsonNode rootNode) {
		List<Field> fields = Gene.getClassSchema().getFields();
		return asStream(rootNode.getElements()).map(node -> {
			Gene gene = new Gene();
			setAllSimpleValues(node, gene.getSchema().getFields(), gene);
			// Arrays.stream(
			// new String[] { "id", "genome", "name", "biotype",
			// "description", "seq_region_name",
			// "taxon_id" }).forEach(
			// elem -> addString(node, elem, gene));
			// // ints
			// Arrays.stream(new String[] { "start", "end", "strand" })
			// .forEach(elem -> addInt(node, elem, gene));
			// // string arrays
			// Arrays.stream(
			// new String[] { "lineage", "GO",
			// "Uniprot_SWISSPROT", "Uniprot_SPTREMBL",
			// "EMBL", "RFAM", "Interpro", "KEGG",
			// "HAMAP", "PANTHER", "UniParc",
			// "scanprosite" }).forEach(
			// elem -> addStrings(node, elem, gene));
			// Homologue
				gene.setHomologues(parseHomologues(node.get("homologues")));
				// Xref
				gene.setXrefs(parseXrefs(node.get("xrefs")));
				// Transcript
				gene.setTranscripts(parseTranscripts(node.get("transcripts")));
				gene.setAnnotations(parseAnnotations(node.get("annotations")));
				return gene;
			}).collect(Collectors.toList());
	}

	protected List<Annotation> parseAnnotations(JsonNode jsonNode) {
		if (jsonNode == null || jsonNode.getElements() == null) {
			return Collections.EMPTY_LIST;
		}
		return asStream(jsonNode.getElements()).map(node -> {
			Annotation x = new Annotation();
			setAllSimpleValues(node, x.getSchema().getFields(), x);
			return x;
		}).collect(Collectors.toList());
	}

	protected List<Xref> parseXrefs(JsonNode jsonNode) {
		return asStream(jsonNode.getElements()).map(
				node -> {
					Xref x = new Xref();
					setAllSimpleValues(node, x.getSchema().getFields(), x);
					x.setLinkageTypes(parseLinkageTypes(jsonNode
							.get("linkage_types")));
					x.setAssociatedXrefs(parseAssociatedXrefs(jsonNode
							.get("associated_xrefs")));
					return x;
				}).collect(Collectors.toList());
	}

	protected List<LinkageType> parseLinkageTypes(JsonNode jsonNode) {
		if (jsonNode == null || jsonNode.getElements() == null) {
			return Collections.EMPTY_LIST;
		}
		return asStream(jsonNode.getElements()).map(node -> {
			LinkageType x = new LinkageType();
			setAllSimpleValues(node, x.getSchema().getFields(), x);
			return x;
		}).collect(Collectors.toList());
	}

	protected List<AssociatedXref> parseAssociatedXrefs(JsonNode jsonNode) {
		if (jsonNode == null || jsonNode.getElements() == null) {
			return Collections.EMPTY_LIST;
		}
		return asStream(jsonNode.getElements()).map(node -> {
			AssociatedXref x = new AssociatedXref();
			setAllSimpleValues(node, x.getSchema().getFields(), x);
			return x;
		}).collect(Collectors.toList());
	}

	protected List<Homologue> parseHomologues(JsonNode jsonNode) {
		return asStream(jsonNode.getElements()).map(
				node -> {
					Homologue h = new Homologue();
					setAllSimpleValues(node, h.getSchema().getFields(), h);
					return h;
				}).collect(Collectors.toList());
	}

	protected List<Transcript> parseTranscripts(JsonNode jsonNode) {
		return asStream(jsonNode.getElements()).map(node -> {
			Transcript t = new Transcript();
			setAllSimpleValues(node, t.getSchema().getFields(), t);
			t.setXrefs(parseXrefs(node.get("xrefs")));
			t.setTranslations(parseTranslations(node.get("translations")));
			return t;
		}).collect(Collectors.toList());
	}

	protected List<Translation> parseTranslations(JsonNode jsonNode) {
		if (jsonNode != null && jsonNode.getElements() != null) {
			return asStream(jsonNode.getElements()).map(
					node -> {
						Translation t = new Translation();
						setAllSimpleValues(node, t.getSchema().getFields(), t);
						t.setXrefs(parseXrefs(node.get("xrefs")));
						t.setProteinFeatures(parseProteinFeatures(node
								.get("protein_features")));
						return t;
					}).collect(Collectors.toList());
		} else {
			return Collections.EMPTY_LIST;
		}
	}

	protected List<ProteinFeature> parseProteinFeatures(JsonNode jsonNode) {
		if (jsonNode == null || jsonNode.getElements() == null) {
			return Collections.EMPTY_LIST;
		}
		return asStream(jsonNode.getElements()).map(node -> {
			ProteinFeature pf = new ProteinFeature();
			setAllSimpleValues(node, pf.getSchema().getFields(), pf);
			return pf;
		}).collect(Collectors.toList());
	}

	public Collection<Gene> parseJsonFile(File file)
			throws JsonProcessingException, IOException {
		log.info("Parsing JSON file " + file.getPath());
		ObjectMapper m = new ObjectMapper();
		Collection<Gene> genes = parseGenes(m.readTree(file));
		log.info("Parsed " + genes.size() + " genes from JSON file "
				+ file.getPath());
		return genes;
	}

	public static void main(String[] args) throws Exception {
		JsonToAvroConverter converter = new JsonToAvroConverter();
		DataFileWriter<Gene> dataFileWriter = new DataFileWriter<Gene>(
				new SpecificDatumWriter<Gene>(Gene.class));
		Logger log = LoggerFactory.getLogger(JsonToAvroConverter.class);
		for (String file : args) {
			File outFile = new File(file.replaceAll("\\.json", ".avro"));
			String parquetFile = file.replaceAll("\\.json", ".parquet");
			FileUtils.deleteQuietly(new File(parquetFile));
			log.info("Parsing " + file + " into " + outFile.getPath());
			log.info("Parsing " + file + " into " + parquetFile);
			dataFileWriter.create(Gene.getClassSchema(), outFile);
			AvroParquetWriter<Gene> parquetWriter = new AvroParquetWriter<Gene>(new Path(parquetFile), Gene.getClassSchema());
			int n = 0;
			for (Gene gene : converter.parseJsonFile(new File(file))) {
				dataFileWriter.append(gene);
				parquetWriter.write(gene);
				n++;
			}
			log.info("Completed writing "+n+" objects");
			parquetWriter.close();
			dataFileWriter.close();
		}
	}

}
