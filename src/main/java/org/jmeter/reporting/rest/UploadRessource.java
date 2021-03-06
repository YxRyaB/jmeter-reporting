package org.jmeter.reporting.rest;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.jmeter.reporting.domain.LoadTest;
import org.jmeter.reporting.domain.LoadTestKey;
import org.jmeter.reporting.domain.Sample;
import org.jmeter.reporting.rest.parts.RestXRequestContext;
import org.jmeter.reporting.service.SampleService;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import restx.RestxRequest;
import restx.annotations.POST;
import restx.annotations.Param;
import restx.annotations.RestxResource;
import restx.factory.Component;
import restx.security.PermitAll;

@Component
@RestxResource
@PermitAll
public class UploadRessource {

	private final LoadTestResource loadTestResource;

	private final SampleService sampleService;

	public UploadRessource(LoadTestResource loadTestResource,
			SampleService sampleService) {
		this.loadTestResource = loadTestResource;
		this.sampleService = sampleService;
	}

	@POST("/upload/{name}/{version}")
	public LoadTest upload(
			String name,
			String version,
			@Param(kind = Param.Kind.CONTEXT, value = "request") RestxRequest request) {
		LoadTest loadTest = null;
		DiskFileItemFactory factory = new DiskFileItemFactory();
		ServletFileUpload upload = new ServletFileUpload(factory);

		List<FileItem> items = null;
		try {
			items = upload.parseRequest(new RestXRequestContext(request));

			// Start load test
			loadTest = loadTestResource.start(name, version);

			// Parse samples
			for (FileItem item : items) {
				// Parse JMeter and create sample
				SAXParserFactory
						.newInstance()
						.newSAXParser()
						.parse(item.getInputStream(),
								new JMeterParser(loadTest.getLoadTestKey()));
			}

			// End load test
			loadTest = loadTestResource.stop(name, version, loadTest
					.getLoadTestKey().getRun());
		} catch (IOException | FileUploadException | SAXException
				| ParserConfigurationException e) {
			System.err.println(e.getMessage());
			// TODO Delete sample
			// TODO Delete loadtest
			// TODO return 500
		} finally {
			if (null != items) {
				for (FileItem item : items) {
					item.delete();
				}
			}
		}
		return loadTest;
	}

	private class JMeterParser extends DefaultHandler {

		private final LoadTestKey loadTestKey;

		private final Set<String> nodesName = new HashSet<>();

		public JMeterParser(LoadTestKey loadTestKey) {
			this.loadTestKey = loadTestKey;
			this.nodesName.add("httpSample");
		}

		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			if (nodesName.contains(localName) || nodesName.contains(qName)) {
				Sample sample = new Sample();
				sample.setLoadTestKey(loadTestKey);

				sample.setElapsedTime(parseInteger(attributes.getValue("t")));
				sample.setLatency(parseLong(attributes.getValue("lt")));
				sample.setTimestamp(parseLong(attributes.getValue("ts")));
				sample.setSuccessful(parseBoolean(attributes.getValue("s")));
				sample.setSampleLabel(attributes.getValue("lb"));
				sample.setResponseCode(attributes.getValue("rc"));
				sample.setResponseData(attributes.getValue("rm"));
				sample.setThreadName(attributes.getValue("tn"));
				sample.setDataType(attributes.getValue("dt"));
				sample.setBytes(parseInteger(attributes.getValue("by")));
				sample.setActiveThreadsGroup(parseInteger(attributes
						.getValue("ng")));
				sample.setActiveThreadsAllGroups(parseInteger(attributes
						.getValue("na")));

				sampleService.save(sample);
			}
			super.startElement(uri, localName, qName, attributes);
		}

		@Override
		public void endDocument() throws SAXException {
			super.endDocument();
		}

		private Long parseLong(String value) {
			return Long.parseLong(value);
		}

		private Integer parseInteger(String value) {
			return Integer.parseInt(value);
		}

		private Boolean parseBoolean(String value) {
			return Boolean.parseBoolean(value);
		}
	}

}
