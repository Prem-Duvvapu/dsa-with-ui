package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Accounts Merge (LeetCode 721), traced on the owner's own accepted submission: a DSU over account
 * indices. The first account to list an email owns it in {@code mailToParent}; a later account
 * listing the same email is unioned with that owner. Then each email goes to its owner's root, and
 * each root's emails are sorted after the name. O(total emails * log) for the sorts.
 *
 * <p>The maps are real {@link HashMap}s, so emails are visited and merged accounts are listed in
 * the same order the submission produces.
 */
@Component
public class AccountsMergeTracer implements AlgorithmTracer {

    private static final String EMAIL = "[A-Za-z0-9_.@]{1,28}";
    private static final String ACCOUNT = "[A-Za-z]{1,12}:" + EMAIL + "(," + EMAIL + "){0,4}";
    private static final String ACCOUNTS = ACCOUNT + "(;" + ACCOUNT + "){0,5}";

    @Override
    public String id() {
        return "accounts-merge";
    }

    @Override
    public DsType dsType() {
        return DsType.DSU;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("accounts", FieldType.STRING)
                        .label("Accounts")
                        .help("One account per ';' as Name:email,email. Up to 6 accounts, 5 emails each.")
                        .length(3, 240)
                        .constraint("pattern", ACCOUNTS)
                        .constraint("patternHint",
                                "Use Name:email,email;Name:email - up to 6 accounts and 5 emails each.")
                        .defaultValue("John:johnsmith@mail.com,john_newyork@mail.com"
                                + ";John:johnsmith@mail.com,john00@mail.com"
                                + ";Mary:mary@mail.com"
                                + ";John:johnnybravo@mail.com")
                        .build());
    }

    /** LeetCode 721's second example: five accounts that share nothing, so nothing merges at all. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("accounts",
                "Gabe:Gabe0@m.co,Gabe3@m.co,Gabe1@m.co"
                        + ";Kevin:Kevin3@m.co,Kevin5@m.co,Kevin0@m.co"
                        + ";Ethan:Ethan5@m.co,Ethan4@m.co,Ethan0@m.co"
                        + ";Hanzo:Hanzo3@m.co,Hanzo1@m.co,Hanzo0@m.co"
                        + ";Fern:Fern5@m.co,Fern1@m.co,Fern0@m.co");
    }

    @Override
    public String annotatedCode() {
        return OwnerDisjointSet.code(Set.of()) + "\n\n" + """
               class Solution {
                   public List<List<String>> accountsMerge(List<List<String>> accounts) {
                       // @a init
                       int n = accounts.size();
                       DisjointSet ds = new DisjointSet(n);
                       Map<String, Integer> mailToParent = new HashMap<>();
                       Map<Integer, List<String>> parentToMails = new HashMap<>();
                       List<List<String>> res = new ArrayList<>();

                       for (int i=0; i<n; i++) {
                           for (int j=1; j<accounts.get(i).size(); j++) {
                               String email = accounts.get(i).get(j);

                               if (!mailToParent.containsKey(email))
                                   // @a first
                                   mailToParent.put(email, i);
                               else
                                   // @a seen
                                   ds.unionBySize(mailToParent.get(email), i);
                           }
                       }

                       for (Map.Entry<String, Integer> m: mailToParent.entrySet()) {
                           String email = m.getKey();
                           int parent = m.getValue();
                           // @a group
                           int ultParent = ds.getUltimateParent(parent);

                           if (!parentToMails.containsKey(ultParent))
                               parentToMails.put(ultParent, new ArrayList<>(Arrays.asList(accounts.get(ultParent).get(0))));

                           parentToMails.get(ultParent).add(email);
                       }

                       for (Map.Entry<Integer, List<String>> m: parentToMails.entrySet()) {
                           List<String> list = m.getValue();
                           // @a sort
                           Collections.sort(list.subList(1,list.size()));
                           res.add(list);
                       }

                       // @a done
                       return res;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String[] entries = in.getString("accounts").split(";");
        int n = entries.length;
        List<List<String>> accounts = new ArrayList<>();
        for (String entry : entries) {
            int colon = entry.indexOf(':');
            List<String> account = new ArrayList<>();
            account.add(entry.substring(0, colon));
            account.addAll(List.of(entry.substring(colon + 1).split(",")));
            accounts.add(account);
        }
        OwnerDisjointSet ds = new OwnerDisjointSet(n);
        Map<String, Integer> mailToParent = new HashMap<>();
        Map<Integer, List<String>> parentToMails = new HashMap<>();
        List<List<String>> res = new ArrayList<>();

        emit.at("init").say("%d account%s, numbered 0 to %d, each in a set of its own. mailToParent remembers "
                        + "the first account that listed each email.", n, Narration.s(n), n - 1)
                .var("Operation", "DisjointSet(" + n + ")").var("Disjoint Sets", ds.sets())
                .var("parent[]", ds.parents()).var("size[]", ds.sizes()).step();

        for (int i = 0; i < n; i++) {
            for (int j = 1; j < accounts.get(i).size(); j++) {
                String email = accounts.get(i).get(j);
                if (!mailToParent.containsKey(email)) {
                    mailToParent.put(email, i);
                    emit.at("first").say("%s appears for the first time, in account %d: mailToParent[%s] = %d.",
                                    email, i, email, i)
                            .var("Operation", "first sight of " + email).var("Disjoint Sets", ds.sets())
                            .var("parent[]", ds.parents()).var("size[]", ds.sizes()).step();
                } else {
                    int owner = mailToParent.get(email);
                    OwnerDisjointSet.Union result = ds.union(owner, i);
                    emit.at("seen").say("%s was already listed by account %d, so accounts %d and %d belong to the "
                                    + "same person. %s", email, owner, owner, i, result.narrate(owner, i))
                            .var("Operation", "unionBySize(" + owner + ", " + i + ")").var("Disjoint Sets", ds.sets())
                            .var("parent[]", ds.parents()).var("size[]", ds.sizes()).step();
                }
            }
        }

        for (Map.Entry<String, Integer> m : mailToParent.entrySet()) {
            String email = m.getKey();
            int parent = m.getValue();
            int ultParent = ds.find(parent, new ArrayList<>());
            if (!parentToMails.containsKey(ultParent)) {
                parentToMails.put(ultParent, new ArrayList<>(List.of(accounts.get(ultParent).get(0))));
            }
            parentToMails.get(ultParent).add(email);
            emit.at("group").say("%s belongs to account %d, whose root is %d: file it under root %d (%s).",
                            email, parent, ultParent, ultParent, accounts.get(ultParent).get(0))
                    .var("Operation", "group " + email).var("Disjoint Sets", ds.sets())
                    .var("parent[]", ds.parents()).var("size[]", ds.sizes()).step();
        }

        for (Map.Entry<Integer, List<String>> m : parentToMails.entrySet()) {
            List<String> list = m.getValue();
            Collections.sort(list.subList(1, list.size()));
            res.add(list);
            emit.at("sort").say("Root %d: sort its emails after the name, giving %s.", m.getKey(), format(List.of(list)))
                    .var("Operation", "sort root " + m.getKey()).var("Disjoint Sets", ds.sets())
                    .var("res", format(res)).var("parent[]", ds.parents()).var("size[]", ds.sizes()).step();
        }

        emit.at("done").say("%d account%s belong to %d %s. Return res.",
                        n, Narration.s(n), res.size(), res.size() == 1 ? "person" : "people")
                .var("Operation", "done").var("Disjoint Sets", ds.sets()).var("res", format(res))
                .var("parent[]", ds.parents()).var("size[]", ds.sizes()).step();
    }

    /** "[John: a, b]; [Mary: c]" - the name, a colon, then the sorted emails. */
    private static String format(List<List<String>> lists) {
        List<String> out = new ArrayList<>();
        for (List<String> list : lists) {
            out.add("[" + list.get(0) + ": " + String.join(", ", list.subList(1, list.size())) + "]");
        }
        return String.join("; ", out);
    }
}
