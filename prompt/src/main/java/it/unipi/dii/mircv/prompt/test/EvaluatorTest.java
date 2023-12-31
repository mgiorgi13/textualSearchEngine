package it.unipi.dii.mircv.prompt.test;

import static org.junit.jupiter.api.Assertions.*;
import it.unipi.dii.mircv.index.structures.Document;
import it.unipi.dii.mircv.index.structures.Lexicon;
import it.unipi.dii.mircv.prompt.query.Searcher;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class EvaluatorTest {
    private static final String DOCUMENTS_PATH = "data/index/documents.bin";
    private static final String LEXICON_PATH = "data/index/lexicon.bin";

    public static void main(String[] args) throws InterruptedException {
        testEvaluators();
    }
    public static void testEvaluators() throws InterruptedException {
        Lexicon lexicon = new Lexicon();
        lexicon.readLexiconFromDisk(-1, LEXICON_PATH);
        ArrayList<Document> documents = Document.readDocumentsFromDisk(-1, DOCUMENTS_PATH);
        Searcher searcher = new Searcher(lexicon, documents);

//        EvaluatorMultiThread evaluatorMT = new EvaluatorMultiThread(lexicon, documents, 10, "disjunctive");
//        evaluatorMT.execute();
//
//        Evaluator evaluator = new Evaluator(searcher, lexicon, documents, 10, "disjunctive");
//        evaluator.execute();

        try (BufferedReader reader = new BufferedReader(new FileReader("data/collection/results.test"));
             BufferedReader readerThread = new BufferedReader(new FileReader("data/trec_eval/results.test"))) {

            String line;
             String lineThread;

             int lineNumber = 0;

            while ((line = reader.readLine()) != null && (lineThread = readerThread.readLine()) != null) {
                lineNumber++;
                // Verifica che le linee siano uguali
                assertEquals(true, line.equals(lineThread), "Line " + lineNumber + " does not match");
            }

            System.out.println("Test passed");

        } catch (IOException e) {
            // Gestisci eccezioni in caso di problemi nell'apertura o lettura dei file
            fail("An error occurred while reading the files: " + e.getMessage());
        }

    }
}
