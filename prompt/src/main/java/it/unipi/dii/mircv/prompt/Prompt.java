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

        Scanner scanner = new Scanner(System.in);
        while(true){
            System.out.println("--------------------------------------------------");
            System.out.println("Welcome to the search engine!");
            System.out.println("MENU: \n - insert 1 to search \n - insert 2 to exit");
            int userInput = scanner.nextInt();
            scanner.nextLine(); // to consume the \n character left by nextInt()
            if (userInput == 1){
                System.out.println("Insert your query ...");
                String queryInput = scanner.nextLine();

                Query query = new Query(queryInput);
                ArrayList<String> queryTerms = query.getQueryTerms();

                Searcher searcher = new Searcher();

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
            else if (userInput == 2){
                System.out.println("Bye!");
                scanner.close();
                break;
            }
            else{
                System.out.println("Wrong input");
            }



        }






    }
}