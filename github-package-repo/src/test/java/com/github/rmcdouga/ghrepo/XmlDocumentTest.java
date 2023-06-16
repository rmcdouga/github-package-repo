package com.github.rmcdouga.ghrepo;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.rmcdouga.ghrepo.XmlDocument.XmlDocumentException;


class XmlDocumentTest {

	private static final String PDFA_DOCUMENT_CONTENTS_1 = "PDFA Document Contents #1";
	private static final String PDFA_DOCUMENT_CONTENTS_2 = "PDFA Document Contents #2";
	private static final byte[] PDFA_DOCUMENT_BYTES_1 = "PDFA Document Bytes #1".getBytes(StandardCharsets.UTF_8);
	private static final byte[] PDFA_DOCUMENT_BYTES_2 = "PDFA Document Bytes #2".getBytes(StandardCharsets.UTF_8);
	private static final String JOB_LOG_DOCUMENT_CONTENTS = "JOB LOG Document Contents";
	private static final byte[] CONVERSION_LOG_DOCUMENT_CONTENTS = "Conversion Log Document Contents".getBytes(StandardCharsets.UTF_8);
	private static final String SAMPLE_XML = "<ToPdfAResult>\n"
			+ "  <ConversionLog>" + Base64.getEncoder().encodeToString(CONVERSION_LOG_DOCUMENT_CONTENTS) + "</ConversionLog>\n"
			+ "  <JobLog>" + JOB_LOG_DOCUMENT_CONTENTS + "</JobLog>\n"
			+ "  <PdfADocument>" + PDFA_DOCUMENT_CONTENTS_1 + "</PdfADocument>\n"
			+ "  <PdfADocument>" + PDFA_DOCUMENT_CONTENTS_2 + "</PdfADocument>\n"
			+ "  <PdfADocumentBase64>" + Base64.getEncoder().encodeToString(PDFA_DOCUMENT_BYTES_1) + "</PdfADocumentBase64>\n"
			+ "  <PdfADocumentBase64>" + Base64.getEncoder().encodeToString(PDFA_DOCUMENT_BYTES_2) + "</PdfADocumentBase64>\n"
			+ "  <IsPdfA>true</IsPdfA>\n"
			+ "</ToPdfAResult>\n";

	private final XmlDocument underTest;
	
	public XmlDocumentTest() {
		try {
			this.underTest = XmlDocument.create(new ByteArrayInputStream(SAMPLE_XML.getBytes()));
		} catch (XmlDocumentException e) {
			throw new IllegalStateException(e);
		}
	}

	@Test
	void testGetString_NotFound() throws Exception {
		assertEquals("", underTest.getString("/ToPdfAResult/NonExistentElement"));
	}

	@Test
	void testGetString() throws Exception {
		assertEquals(JOB_LOG_DOCUMENT_CONTENTS, underTest.getString("/ToPdfAResult/JobLog"));
	}

	@Test
	void testGetBoolean() throws Exception {
		assertEquals(true, underTest.getBoolean("/ToPdfAResult/IsPdfA"));
	}

	@Test
	void testGetMultipleStrings() throws Exception {
		List<String> result = underTest.getStrings("/ToPdfAResult/PdfADocument");
		assertEquals(2, result.size());
		assertAll(
				()->assertEquals(PDFA_DOCUMENT_CONTENTS_1, result.get(0)),
				()->assertEquals(PDFA_DOCUMENT_CONTENTS_2, result.get(1))
				);
	}

}
