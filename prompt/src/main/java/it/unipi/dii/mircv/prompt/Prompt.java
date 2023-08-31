package it.unipi.dii.mircv.prompt;


import it.unipi.dii.mircv.index.structures.Document;
import it.unipi.dii.mircv.index.structures.Lexicon;
import it.unipi.dii.mircv.prompt.query.Query;
import it.unipi.dii.mircv.prompt.query.Searcher;
import it.unipi.dii.mircv.prompt.structure.QueryResult;

import javax.print.Doc;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Scanner;

public class Prompt {

    private static int n_results = 10; // number of documents to return for a query

    public static void main(String[] args) {

        // load main structure in memory
        Lexicon lexicon = new Lexicon();
        lexicon.readLexiconFromDisk(-1);
        ArrayList<Document> documents = Document.readDocumentsFromDisk(-1);

        System.out.println("Insert your query ...");
        Scanner scanner = new Scanner(System.in);
        String userInput = scanner.nextLine();
        scanner.close();

        Query query = new Query(userInput);
        ArrayList<String> queryTerms = query.getQueryTerms();

//        // print query terms
//        System.out.println("User Query terms:");
//        for (String term : queryTerms) {
//            System.out.println(term);
//        }

//        Searcher searcher = new Searcher(lexicon, documents);
        Searcher searcher = new Searcher();
        ArrayList<String> pid_results = searcher.search(queryTerms, lexicon, documents);

        if (pid_results.size() != 0) {
            System.out.println("PID results:");
            for (String pid : pid_results) {
                System.out.println(pid);
            }
        } else {
            System.out.println("No results found");
        }

        System.out.println("disjunctive");
        ArrayList<QueryResult> results;
        searcher.DAAT(queryTerms, lexicon, documents, n_results, "disjunctive");
        results = searcher.getQueryResults();
        System.out.println(results);

        System.out.println("conjunctive");
        searcher.DAAT(queryTerms, lexicon, documents, n_results, "conjunctive");
        results = searcher.getQueryResults();
        System.out.println(results);

    }
}