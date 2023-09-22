package it.unipi.dii.mircv.prompt.query;

import it.unipi.dii.mircv.index.structures.*;
import it.unipi.dii.mircv.prompt.structure.QueryResult;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.*;

public class Searcher {

    //    private String term;
    private Lexicon lexicon;
    private ArrayList<Document> documents;
    private ArrayList<QueryResult> queryResults;
    private ArrayList<String> previousQueryTerms;
    private ArrayList<BlockDescriptorList> blockDescriptorList;
    private ArrayList<PostingList> postingLists = null;
    private String previousMode;
    private String previousScoringFunction;
    private double AVG_DOC_LENGTH;
    private static int N_docs = 0; // number of documents in the collection
    private static final int NUMBER_OF_POSTING = 10;
    private static final int BLOCK_POSTING_LIST_SIZE = (4 * 2) * NUMBER_OF_POSTING; // 4 byte per docID, 4 byte per freq and postings
    private static final int POSTING_LIST_SIZE = (4 * 2); // 4 byte per docID, 4 byte per freq
    private static final String BLOCK_DESCRIPTOR_PATH = "data/index/blockDescriptor.bin";
    private static final String INDEX_PATH = "data/index/index.bin";

    public Searcher(Lexicon lexicon, ArrayList<Document> documents) {
        this.queryResults = new ArrayList<>();
        this.postingLists = new ArrayList<>();
        this.previousQueryTerms = new ArrayList<>();
        this.blockDescriptorList = new ArrayList<>();
        this.previousMode = "";
        this.previousScoringFunction = "";
        this.lexicon = lexicon;
        this.documents = documents;
        //read number of docs from disk
        try (FileInputStream fileIn = new FileInputStream("data/index/documentInfo.bin");
             ObjectInputStream in = new ObjectInputStream(fileIn)) {
            this.N_docs = (int) in.readObject();
            long totDocLength = (long) in.readObject();
            AVG_DOC_LENGTH = totDocLength / N_docs;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    public void DAAT(ArrayList<String> queryTerms, int K, String mode, String scoringFunction) {
//        if ((this.previousQueryTerms.equals(queryTerms) &&
//                this.previousScoringFunction.equals(scoringFunction) &&
//                (this.previousMode.equals(mode) || queryTerms.size() == 1))) // same query as before
//            return;
//        //process query and clear previous results
//        this.previousQueryTerms = new ArrayList<>(queryTerms);
//        this.previousMode = mode;
//        this.previousScoringFunction = scoringFunction;
//        this.queryResults.clear();
//
//        int minDocId;
//        ArrayList<Integer> indexes = new ArrayList<>();
//        ArrayList<Double> scores = new ArrayList<>();
//        ArrayList<Integer> blocksNumber = new ArrayList<>();
//
//        LinkedHashMap<String, LexiconElem> queryTermsMap = new LinkedHashMap<>();
//        for (String term : queryTerms) {
//            if (lexicon.getLexicon().containsKey(term)) {
//                queryTermsMap.put(term, lexicon.getLexiconElem(term));
//            } else if (!lexicon.getLexicon().containsKey(term) && mode.equals("conjunctive")) {
//                return;
//            }
//        }
//
//        if (queryTermsMap.size() == 0)
//            return;
//
//        //initialize posting list for query terms
//        initializePostingListForQueryTerms(queryTermsMap, blocksNumber);
//
//        if (postingLists.size() == 0)
//            return; // if no terms in query are in lexicon means that there are no results
//
//        do {
//            scores.clear();
//            indexes.clear();
//            // get min docID from posting list iterators and indexes of posting list iterators with min docID
//            minDocId = getNextDocIdDAAT(indexes);
//
//            if (minDocId == Integer.MAX_VALUE)
//                break;
//
//            if (mode.equals("conjunctive") && indexes.size() != postingLists.size()) {
//                for (Integer i : indexes) {
//
//                    //update posting list
//                    updatePosting(postingLists.get(i), i);
//                }
//                continue;
//            }
//
//            double document_score = 0;
//
//            for (Integer i : indexes) {
//                //calculate score for posting list with min docID
//                if (scoringFunction.equals("TFIDF"))
//                    scores.add(tfidf(postingLists.get(i).getFreq(), lexicon.getLexiconElem(queryTerms.get(i)).getDf()));
//                else if (scoringFunction.equals("BM25"))
//                    scores.add(BM25(postingLists.get(i).getFreq(), lexicon.getLexiconElem(queryTerms.get(i)).getDf(), documents.get(minDocId).getLength(), AVG_DOC_LENGTH));
//                //update posting list
//                updatePosting(postingLists.get(i), i);
//            }
//
//            // Sum all the values of scores
//            for (double score : scores) {
//                document_score += score;
//            }
//            if (document_score > 0) {
//                // Get document
//                Document document = documents.get(minDocId);
//                // Get document pid
//                String pid = document.getDocNo();
//                // Add pid to results
//                document_score = Math.round(document_score * 10e5) / 10e5;
//                queryResults.add(new QueryResult(pid, document_score));
//            }
//
//        } while (true);
//
//        Collections.sort(queryResults);
//        if (queryResults.size() > K) {
//            queryResults = new ArrayList<>(queryResults.subList(0, K));
//        }
//
//        postingLists.clear();
//
//    }

    public void DAAT(ArrayList<String> queryTerms, int K, String mode, String scoringFunction) {
//        if ((this.previousQueryTerms.equals(queryTerms) && this.previousMode.equals(mode) && this.previousScoringFunction.equals(scoringFunction))
//                || (this.previousQueryTerms.equals(queryTerms) && queryTerms.size() == 1 && this.previousScoringFunction.equals(scoringFunction)))) // same query as before
        if ((this.previousQueryTerms.equals(queryTerms) &&
                this.previousScoringFunction.equals(scoringFunction) &&
                (this.previousMode.equals(mode) || queryTerms.size() == 1))) // same query as before
            return;
        //process query and clear previous results
        this.previousQueryTerms = new ArrayList<>(queryTerms);
        this.previousMode = mode;
        this.previousScoringFunction = scoringFunction;
        this.queryResults.clear();

        long firstBlockOffset;
        int blocksNumber;
        int minDocId;
        ArrayList<Integer> indexes = new ArrayList<>();
        ArrayList<Double> scores = new ArrayList<>();

        if (mode.equals("conjunctive")) {
            for (String term : queryTerms)
                if (!lexicon.getLexicon().containsKey(term))
                    return;
        }

        // for each term in query get all block descriptors and add them to blockDescriptorIterators
        for (String term : queryTerms) {
            if (lexicon.getLexicon().containsKey(term)) {
                firstBlockOffset = lexicon.getLexiconElem(term).getOffset();
                BlockDescriptor firstBlockDescriptor = BlockDescriptor.readFirstBlock(firstBlockOffset, BLOCK_DESCRIPTOR_PATH); // read first block descriptor, used because MaxScore is not implemented
//                blocksNumber = lexicon.getLexiconElem(term).getBlocksNumber();
//                blockDescriptorIterators.add(openBlocks(firstBlockOffset, blocksNumber).iterator());
                // load total posting list for the term, used because MaxScore is not implemented
                PostingList postingList = new PostingList();
                postingList.readPostingList(-1, lexicon.getLexiconElem(term).getDf(), firstBlockDescriptor.getPostingListOffset(), INDEX_PATH);
                postingList.openList();
                postingLists.add(postingList); // add postinglist of the term to postingListIterators
                //TODO SOLO PER DEBUG
//                System.out.println("Term: " + term + " postlist: " + postingList.getPostingListSize());

            } else {
                // if term is not in lexicon add empty posting list
                postingLists.add(null);

            }
        }

        if (postingLists.size() == 0)
            return; // if no terms in query are in lexicon means that there are no results

        do {
            scores.clear();
            indexes.clear();
            // get min docID from posting list iterators and indexes of posting list iterators with min docID
            minDocId = getNextDocId(indexes);

            if (minDocId == Integer.MAX_VALUE)
                break;

            if (mode.equals("conjunctive") && indexes.size() != postingLists.size()) {
                for (Integer i : indexes) {
                    postingLists.get(i).next();
                }
                continue;
            }

            double document_score = 0;

            for (Integer i : indexes) {
                //calculate score for posting list with min docID
                double score = 0;
                if (scoringFunction.equals("TFIDF"))
                    score = tfidf(postingLists.get(i).getFreq(), lexicon.getLexiconElem(queryTerms.get(i)).getDf());
                else if (scoringFunction.equals("BM25"))
                    score = BM25(postingLists.get(i).getFreq(), lexicon.getLexiconElem(queryTerms.get(i)).getDf(), documents.get(minDocId).getLength(), AVG_DOC_LENGTH);
                // get next posting from posting list with min docID
                scores.add(score);
                postingLists.get(i).next();
            }

            // Sum all the values of scores
            for (double score : scores) {
                document_score += score;
            }
            if (document_score > 0) {
                // Get document
                Document document = documents.get(minDocId);
                // Get document pid
                String pid = document.getDocNo();
                // Add pid to results
                document_score = Math.round(document_score * 10e5) / 10e5;
                queryResults.add(new QueryResult(pid, document_score));
            }

        } while (true);

        Collections.sort(queryResults);
        if (queryResults.size() > K) {
            queryResults = new ArrayList<>(queryResults.subList(0, K));
        }

        for (PostingList pi : postingLists) {
            if (pi != null)
                pi.closeList();
        }
        postingLists.clear();

    }

    public void maxScore(ArrayList<String> queryTerms, int K, String mode, String scoringFunction) {
        if ((this.previousQueryTerms.equals(queryTerms) && this.previousScoringFunction.equals(scoringFunction) && (this.previousMode.equals(mode) || queryTerms.size() == 1))) // same query as before
            return;
        //process query and clear previous results
        this.previousQueryTerms = new ArrayList<>(queryTerms);
        this.previousMode = mode;
        this.previousScoringFunction = scoringFunction;
        this.queryResults.clear();

        ArrayList<Integer> blocksNumber = new ArrayList<>();
        int essential_index = 0;
        double current_threshold = 0, partial_score, DUB;

        //fai un sort su query terms in base a TUB
        LinkedHashMap<String, LexiconElem> queryTermsMap = new LinkedHashMap<>();
        for (String term : queryTerms) {
            if (lexicon.getLexicon().containsKey(term)) {
                queryTermsMap.put(term, lexicon.getLexiconElem(term));
            } else if (!lexicon.getLexicon().containsKey(term) && mode.equals("conjunctive")) {
                return;
            }
        }

        if (queryTermsMap.size() == 0)
            return;

        queryTermsMap = Lexicon.sortLexicon(queryTermsMap, scoringFunction);

        //initialize posting list for query terms
        initializePostingListForQueryTerms(queryTermsMap, blocksNumber);

        // finche ho essential posting list
        do {
            // get next docid to be processed
            int new_essential_index = -2;
            int docid = getNextDocIdMAXSCORE(essential_index);
//            System.out.println("docid: " + docid);
            if (docid == Integer.MAX_VALUE)
                break;

            partial_score = computeEssentialPS(essential_index, scoringFunction, docid, queryTermsMap, mode); // compute partial score for docID into essential posting list

            if (current_threshold != 0)
                DUB = sumNonEssentialTUBs(essential_index, partial_score, scoringFunction, queryTermsMap);
            else
                DUB = partial_score;

            //controllo se DUB > current_threshold
            if (DUB > current_threshold) {
                partial_score = computeDUB(essential_index, docid, scoringFunction, partial_score, DUB, current_threshold, blocksNumber, queryTermsMap, mode);
            }
            if (partial_score > current_threshold) {
                // Get document
                Document document = documents.get(docid);
                // Get document pid
                String pid = document.getDocNo();
                // Add pid to results
                partial_score = Math.round(partial_score * 10e5) / 10e5;
                queryResults.add(new QueryResult(pid, partial_score));
                // SE queryresult.size > k
                if (queryResults.size() >= K) {
                    Collections.sort(queryResults);
                    if (queryResults.size() > K)
                        // Rimuovi ultimo elemento
                        queryResults.remove(queryResults.size() - 1);
                    // Aggiorna current_threshold
                    current_threshold = queryResults.get(queryResults.size() - 1).getScoring();
                    new_essential_index = compute_essential_index(queryTermsMap, scoringFunction, current_threshold);
                }
            }
            if (new_essential_index != -2)
                essential_index = new_essential_index;
        } while (essential_index != -1);
        Collections.sort(queryResults);
        blockDescriptorList.clear();
        postingLists.clear();
    }

    private double computeDUB(int essential_index, int docid, String scoringFunction, double partial_score, double DUB, double current_threshold, ArrayList<Integer> blocksNumber, HashMap<String, LexiconElem> queryTermsMap, String mode) {
        ArrayList<String> termList = new ArrayList<>(queryTermsMap.keySet());
        for (int j = essential_index - 1; j >= 0; j--) {
            Posting p = postingLists.get(j).nextGEQ(docid, blockDescriptorList.get(j), blocksNumber.get(j), INDEX_PATH);

            // nextGEQ return null if docid not found in posting list
            if (p == null && mode.equals("conjunctive"))
                return 0;
            else if (p == null)
                continue;

            if (scoringFunction.equals("TFIDF"))
                DUB -= queryTermsMap.get(termList.get(j)).getTUB_tfidf();
            else if (scoringFunction.equals("BM25"))
                DUB -= queryTermsMap.get(termList.get(j)).getTUB_bm25();

            // nextGEQ return a posting with docid != docid
            if (p.getDocID() != docid && mode.equals("conjunctive"))
                return 0;
            else if (p.getDocID() != docid)
                continue;

            // docid found in posting list
            double result = 0;
            if (scoringFunction.equals("TFIDF")) {
                result = tfidf(p.getFreq(), lexicon.getLexiconElem(termList.get(j)).getDf());
                partial_score += result;
                DUB += result;
            } else if (scoringFunction.equals("BM25")) {
                result = BM25(p.getFreq(), lexicon.getLexiconElem(termList.get(j)).getDf(), documents.get(docid).getLength(), AVG_DOC_LENGTH);
                partial_score += result;
                DUB += result;
            }
            if (DUB < current_threshold)
                break;

        }
        return partial_score;
    }

    private double sumNonEssentialTUBs(int essential_index, double partial_score, String scoringFunction, HashMap<String, LexiconElem> queryTermsMap) {
        // calcolo DUB
        double DUB = partial_score;
        ArrayList<String> termList = new ArrayList<>(queryTermsMap.keySet());
        for (int j = 0; j < essential_index; j++) {
            if (scoringFunction.equals("TFIDF"))
                DUB += queryTermsMap.get(termList.get(j)).getTUB_tfidf();
            else if (scoringFunction.equals("BM25"))
                DUB += queryTermsMap.get(termList.get(j)).getTUB_bm25();
        }

        return DUB;
    }

    private double computeEssentialPS(int essential_index, String scoringFunction, int docid, HashMap<String, LexiconElem> queryTermsMap, String mode) {
        double partial_score = 0;
        ArrayList<String> termList = new ArrayList<>(queryTermsMap.keySet());
        boolean notFound = false;


        for (int j = essential_index; j < postingLists.size(); j++) {
            PostingList pl = postingLists.get(j);
            if (pl.getPostingIterator() == null || pl.getDocId() != docid) {
                if (mode.equals("conjunctive"))
                    notFound = true;
                continue;
            }
            if (scoringFunction.equals("TFIDF"))
                partial_score += tfidf(pl.getFreq(), lexicon.getLexiconElem(termList.get(j)).getDf());
            else if (scoringFunction.equals("BM25"))
                partial_score += BM25(pl.getFreq(), lexicon.getLexiconElem(termList.get(j)).getDf(), documents.get(docid).getLength(), AVG_DOC_LENGTH);
            //update posting list

            updatePosting(pl, j);
        }
        if (notFound)
            return 0;
        return partial_score;
    }

    private void updatePosting(PostingList pl, int j) {

        if (pl.hasNext()) // ho ancora post nella posting list attuale
            pl.next();
        else if (!pl.hasNext() && blockDescriptorList.get(j).hasNext()) { //devo caricare un altro blocco
            blockDescriptorList.get(j).next();
            pl.readPostingList(-1, blockDescriptorList.get(j).getNumPosting(), blockDescriptorList.get(j).getPostingListOffset(), INDEX_PATH);
            pl.openList();
            pl.next();
        } else {
            pl.closeList();
        }
    }

    private void initializePostingListForQueryTerms
            (HashMap<String, LexiconElem> queryTermsMap, ArrayList<Integer> blocksNumber) {
        int i = 0;
        long firstBlockOffset;
        for (String term : queryTermsMap.keySet()) {
            firstBlockOffset = lexicon.getLexiconElem(term).getOffset();
            blocksNumber.add(lexicon.getLexiconElem(term).getBlocksNumber());
            //read all blocks
            blockDescriptorList.add(new BlockDescriptorList(firstBlockOffset, blocksNumber.get(i), BLOCK_DESCRIPTOR_PATH));
//            DebugPostingList(blockDescriptorList.get(i), blocksNumber.get(i));
            blockDescriptorList.get(i).openBlock();
            blockDescriptorList.get(i).next();
            //load first posting list for the term
            PostingList postingList = new PostingList();
            postingList.readPostingList(-1, blockDescriptorList.get(i).getNumPosting(), blockDescriptorList.get(i).getPostingListOffset(), INDEX_PATH);
            postingList.openList();
            postingLists.add(postingList); // add postinglist of the term to postingListIterators
            postingLists.get(i).next();
            i++;
        }
    }

    private int compute_essential_index(HashMap<String, LexiconElem> queryTermsMap, String scoringFunction,
                                        double current_threshold) {
        if (current_threshold == 0)
            return 0;

        int essential_index = 0;
        double TUBsum = 0;
        boolean essential_postings_found = false;
        for (String term : queryTermsMap.keySet()) {
            if (scoringFunction.equals("BM25"))
                TUBsum += queryTermsMap.get(term).getTUB_bm25();
            else if (scoringFunction.equals("TFIDF"))
                TUBsum += queryTermsMap.get(term).getTUB_tfidf();
            if (TUBsum < current_threshold) {
                essential_index++;
            } else {//essential postings found
                essential_postings_found = true;
                break;
            }
        }
        if (essential_postings_found)
            return essential_index;
        else
            return -1;
    }

    private double tfidf(int tf, int df) {
        double score = 0;
        if (tf > 0)
            score = (1 + Math.log(tf)) * Math.log(N_docs / df);
        return score;
    }

    private double BM25(int tf, int df, int docLength, double avgDocLength) {
        //TODO CONTROLLARE LA FORMULA
        double score;
        double k1 = 1.2;
        double b = 0.75;

        double B = ((1 - b) + b * (docLength / avgDocLength));
        double idf = Math.log((N_docs) / (df));
        score = (tf / (k1 * B + tf)) * idf;
        return score;
    }

    private int getNextDocIdDAAT(ArrayList<Integer> indexes) {
        int min = Integer.MAX_VALUE;

        for (int i = 0; i < postingLists.size(); i++) {
            PostingList postList = postingLists.get(i);
            if (postList.getPostingIterator() == null)
                continue;

            if (postList.getDocId() < min) {
                indexes.clear();
                min = postList.getDocId();
                indexes.add(i);
            } else if (postList.getDocId() == min) {
                indexes.add(i);
            }
        }
        return min;
    }

    private int getNextDocIdMAXSCORE(int essential_index) {
        int min = Integer.MAX_VALUE;

        for (int i = essential_index; i < postingLists.size(); i++) {
            PostingList postList = postingLists.get(i);
            if (postList.getPostingIterator() == null)
                continue;
            if (postList.getDocId() < min)
                min = postList.getDocId();
        }
        return min;
    }

    public void printResults(long time) {
        if (queryResults == null || queryResults.size() == 0) {
            System.out.println("Unfortunately, no documents were found for your query.");
            return;
        }

        System.out.println("These " + queryResults.size() + " documents may are of your interest");
        System.out.println(queryResults);
        System.out.println("Search time: " + time + " ms");
    }

    public ArrayList<QueryResult> getQueryResults() {
        return this.queryResults;
    }

    public void Debug(int docid, double current_threshold, int essential_index, LinkedHashMap<
            String, LexiconElem> queryTermsMap) {

        System.out.println("------------------");
        for (String term : queryTermsMap.keySet()) {
            System.out.println(term + " " + queryTermsMap.get(term).getTUB_bm25());
        }
        System.out.println(docid + " " + current_threshold + " " + essential_index);
        for (int i = 0; i < postingLists.size(); i++) {
            System.out.println(postingLists.get(i));
        }
        System.out.println("------------------");
    }

    public void DebugPostingList(BlockDescriptorList bdl, int blocksNumber) {
        System.out.println("------------------");
        PostingList pl = new PostingList();
        bdl.openBlock();
        int sum = 0;
        for (int i = 0; i < blocksNumber; i++) {
            System.out.println("Block " + i);
            bdl.next();
            pl.readPostingList(-1, bdl.getNumPosting(), bdl.getPostingListOffset(), INDEX_PATH);
            sum += pl.getPostingListSize();
        }
        System.out.println(sum);
    }

    private int getNextDocId(ArrayList<Integer> indexes) {
        int min = Integer.MAX_VALUE;

        for (int i = 0; i < postingLists.size(); i++) {
            PostingList postList = postingLists.get(i);
            if (postList == null) // term not in lexicon
                continue;
            if (postList.getActualPosting() == null) { //first lecture
                if (postList.hasNext())
                    postList.next();
                else // no more docs for this term
                    continue;
            }

            if (postList.getDocId() < min) {
                indexes.clear();
                min = postList.getDocId();
                indexes.add(i);
            } else if (postList.getDocId() == min) {
                indexes.add(i);
            }
        }
        return min;
    }

}


// gram maltos molecular weight introduction project manhattan bomb war civil japan