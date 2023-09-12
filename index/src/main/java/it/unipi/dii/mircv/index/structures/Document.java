package it.unipi.dii.mircv.index.structures;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Document {
    private static final int DOCNO_LENGTH = 64;

    private int docID;
    private String docNo;
    private String URL;
    private String PR;
    private String body;
    private int length;
    private String rawDocument;
    private double DUB_bm25; // document upper bound for bm25
    private double DUB_tfidf; // document upper bound for tfidf

    public Document(String rawDocument, int docID) {
        this.docID = docID;
        this.rawDocument = rawDocument;
        length = 0;
        parseDocument();
    }

    public Document(int docID, String docNo, int length) {
        this.docID = docID;
        this.docNo = docNo;
        this.length = length;
    }

    public Document() {

    }

    private void parseDocument() {
        // parse the raw document and set id and body
        String split[] = rawDocument.split("\t");
        this.docNo = split[0]; // TODO CONTROLLARE SE è VERAMENTE L'ID, docno != da pid
        this.body = split[1];
    }

    @Override
    public String toString() {
        return "Document{" +
                "docID=" + docID +
                ", docNo='" + docNo + '\'' +
                ", length=" + length +
                '}';
    }

    public int getDocID() {
        return this.docID;
    }

    public String getDocNo() {
        return docNo;
    }

    public int getLength() {
        return length;
    }

    public String getBody() {
        return this.body;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public double getDUB_bm25() {
        return DUB_bm25;
    }

    public void setDUB_bm25(double DUB_bm25) {
        this.DUB_bm25 = DUB_bm25;
    }

    public double getDUB_tfidf() {
        return DUB_tfidf;
    }

    public void setDUB_tfidf(double DUB_tfidf) {
        this.DUB_tfidf = DUB_tfidf;
    }

    public static void saveDocumentsToDisk(ArrayList<Document> docs, int index) {
        String filePath = "data/index/documents/documents_" + index + ".bin";

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(filePath, true);
            FileChannel fileChannel = fileOutputStream.getChannel();

            // Creare un buffer ByteBuffer per migliorare le prestazioni di scrittura
            ByteBuffer buffer = ByteBuffer.allocate(1024);

            for (Document doc : docs) {
                buffer.putInt(doc.getDocID());

                String docNo = doc.getDocNo();
                byte[] docNoBytes = docNo.getBytes(StandardCharsets.UTF_8);
                if (docNoBytes.length > DOCNO_LENGTH) {
                    throw new IllegalArgumentException("Document number exceeds maximum length.");
                }

                // Scrivi la stringa come array di byte, riempita o tagliata per adattarsi alla dimensione fissa
                byte[] paddedDocNoBytes = new byte[DOCNO_LENGTH];
                System.arraycopy(docNoBytes, 0, paddedDocNoBytes, 0, docNoBytes.length);
                buffer.put(paddedDocNoBytes);

                buffer.putInt(doc.getLength());

                // Se il buffer è pieno, scrivi il suo contenuto sul file
                //if (!buffer.hasRemaining()) {
                if (buffer.remaining() < (paddedDocNoBytes.length + 4 + 4)) {
                    buffer.flip();
                    fileChannel.write(buffer);
                    buffer.clear();
                }
            }

            // Scrivi eventuali dati rimanenti nel buffer sul file
            if (buffer.position() > 0) {
                buffer.flip();
                fileChannel.write(buffer);
            }

            // Chiudi le risorse
            fileChannel.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<Document> readDocumentsFromDisk(int index) {
        String filePath;

        if (index == -1) {
            filePath = "data/index/documents.bin";
        } else {
            filePath = "data/index/documents/documents_" + index + ".bin";
        }

        ArrayList<Document> documents = new ArrayList<>();

        try {
            FileInputStream fileInputStream = new FileInputStream(filePath);
            FileChannel fileChannel = fileInputStream.getChannel();

            // Creare un buffer ByteBuffer per migliorare le prestazioni di lettura
            ByteBuffer buffer = ByteBuffer.allocate(1024); // Usiamo un buffer di 1024 byte

            while (fileChannel.read(buffer) > 0) {
                buffer.flip();

                while (buffer.remaining() >= (DOCNO_LENGTH + 4 + 4)) {
                    Document doc = new Document();
                    doc.docID = buffer.getInt();

                    byte[] docNoBytes = new byte[DOCNO_LENGTH];
                    buffer.get(docNoBytes);
                    doc.docNo = new String(docNoBytes, StandardCharsets.UTF_8).trim();

                    doc.length = buffer.getInt();

                    documents.add(doc);
                }

                buffer.compact();
            }

            // Chiudi le risorse
            fileChannel.close();
            fileInputStream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return documents;
    }

    public static void ConcatenateFiles(ArrayList<String> fileNames, String outputFileName) {
        try {
            FileOutputStream outputStream = new FileOutputStream(outputFileName);
            FileChannel outputChannel = outputStream.getChannel();

            for (String fileName : fileNames) {
                FileInputStream inputStream = new FileInputStream(fileName);
                FileChannel inputChannel = inputStream.getChannel();
                outputChannel.transferFrom(inputChannel, outputChannel.size(), inputChannel.size());
                inputChannel.close();
                inputStream.close();
            }

            outputChannel.close();
            outputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
