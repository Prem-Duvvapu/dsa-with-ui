package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Two accounts belong to the same person as soon as they share ONE email, and that relation
 * is transitive - A shares an email with B, B shares a different email with C, so all three
 * are one person even though A and C have nothing in common. That is exactly connectivity,
 * so the merge is a DSU over ACCOUNT INDICES (not over emails): the first account to list an
 * email owns it, and every later account listing the same email unions itself with the owner.
 * Grouping by root at the end and sorting each group's distinct emails gives the answer.
 *
 * <p>Elements are 1-indexed to match {@code DsuCanvas}; index 0 is unused padding.
 *
 * <p>Accounts travel as one {@link FieldType#STRING} in {@code Name:email,email;Name:email}
 * form rather than as a new field kind: names are letters and emails cannot contain a comma,
 * a semicolon or a colon, so the separators are unambiguous, and the existing
 * {@code .constraint("pattern", ...)} mechanism bounds both the account count and the emails
 * per account with bounded regex repetition without touching {@link InputValidator}.
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
        return """
               public List<List<String>> accountsMerge(List<List<String>> accounts) {
                   int n = accounts.size();
                   int[] parent = new int[n + 1];
                   int[] rank = new int[n + 1];
                   // @a init
                   for (int i = 0; i <= n; i++) parent[i] = i;
                   Map<String, Integer> owner = new HashMap<>();

                   for (int i = 1; i <= n; i++) {
                       List<String> row = accounts.get(i - 1);
                       for (String email : row.subList(1, row.size())) {
                           if (!owner.containsKey(email)) {
                               // @a claim
                               owner.put(email, i);
                           } else {
                               // @a union
                               union(i, owner.get(email), parent, rank);
                           }
                       }
                   }

                   Map<Integer, TreeSet<String>> groups = new TreeMap<>();
                   for (Map.Entry<String, Integer> e : owner.entrySet()) {
                       groups.computeIfAbsent(find(e.getValue(), parent),
                                              k -> new TreeSet<>()).add(e.getKey());
                   }

                   List<List<String>> merged = new ArrayList<>();
                   for (Map.Entry<Integer, TreeSet<String>> g : groups.entrySet()) {
                       // @a merge
                       List<String> row = new ArrayList<>();
                       row.add(nameOfFirstAccountIn(g.getKey()));
                       row.addAll(g.getValue());
                       merged.add(row);
                   }

                   // @a done
                   return merged;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String[] entries = in.getString("accounts").split(";");
        int n = entries.length;
        String[] names = new String[n];
        List<List<String>> emails = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int colon = entries[i].indexOf(':');
            names[i] = entries[i].substring(0, colon);
            emails.add(List.of(entries[i].substring(colon + 1).split(",")));
        }

        int[] parent = new int[n + 1];
        int[] rank = new int[n + 1];
        for (int i = 0; i <= n; i++) parent[i] = i;

        emit.at("init")
                .say("%d account(s). Each starts in its own set - two accounts only merge once "
                        + "they are found to share an email.", n)
                .var("parent[]", Arrays.toString(parent)).var("rank[]", Arrays.toString(rank))
                .var("Disjoint Sets", formatSets(parent, n))
                .var("Operation", "Initialize DSU(" + n + ")")
                .step();

        Map<String, Integer> owner = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            int acc = i + 1;
            for (String email : emails.get(i)) {
                if (!owner.containsKey(email)) {
                    owner.put(email, acc);
                    emit.at("claim")
                            .say("Account %d ('%s') is the first to list %s - it becomes that email's owner.",
                                    acc, names[i], email)
                            .var("parent[]", Arrays.toString(parent)).var("rank[]", Arrays.toString(rank))
                            .var("Disjoint Sets", formatSets(parent, n))
                            .var("Operation", "claim " + email + " -> account " + acc)
                            .step();
                    continue;
                }

                int other = owner.get(email);
                int ru = find(acc, parent);
                int rv = find(other, parent);
                String reason;
                if (ru == rv) {
                    reason = String.format(
                            "accounts %d and %d already share root %d, so there is nothing to merge.",
                            acc, other, ru);
                } else if (rank[ru] < rank[rv]) {
                    parent[ru] = rv;
                    reason = String.format("rank[%d] (%d) < rank[%d] (%d), so root %d joins root %d.",
                            ru, rank[ru], rv, rank[rv], ru, rv);
                } else if (rank[ru] > rank[rv]) {
                    parent[rv] = ru;
                    reason = String.format("rank[%d] (%d) < rank[%d] (%d), so root %d joins root %d.",
                            rv, rank[rv], ru, rank[ru], rv, ru);
                } else {
                    parent[rv] = ru;
                    rank[ru]++;
                    reason = String.format("ranks tie, so root %d joins root %d and rank[%d] becomes %d.",
                            rv, ru, ru, rank[ru]);
                }

                emit.at("union")
                        .say("Account %d ('%s') also lists %s, already owned by account %d: %s",
                                acc, names[i], email, other, reason)
                        .var("parent[]", Arrays.toString(parent)).var("rank[]", Arrays.toString(rank))
                        .var("Disjoint Sets", formatSets(parent, n))
                        .var("Operation", "union(" + acc + ", " + other + ")")
                        .step();
            }
        }

        Map<Integer, TreeSet<String>> groups = new TreeMap<>();
        for (Map.Entry<String, Integer> e : owner.entrySet()) {
            groups.computeIfAbsent(find(e.getValue(), parent), k -> new TreeSet<>()).add(e.getKey());
        }
        Map<Integer, Integer> firstAccount = new HashMap<>();
        for (int i = 1; i <= n; i++) {
            firstAccount.merge(rootOf(i, parent), i, Math::min);
        }

        List<String> merged = new ArrayList<>();
        for (Map.Entry<Integer, TreeSet<String>> g : groups.entrySet()) {
            int root = g.getKey();
            int first = firstAccount.get(root);
            String name = names[first - 1];
            String sorted = String.join(", ", g.getValue());
            merged.add("[" + name + ": " + sorted + "]");

            emit.at("merge")
                    .say("Root %d covers accounts {%s}. Its %d distinct email(s) sort to %s, and the "
                                    + "name comes from account %d ('%s').",
                            root, membersOf(parent, n, root), g.getValue().size(), sorted, first, name)
                    .var("parent[]", Arrays.toString(parent)).var("rank[]", Arrays.toString(rank))
                    .var("Disjoint Sets", formatSets(parent, n))
                    .var("Operation", "merge group " + root)
                    .var("Merged", "[" + name + ": " + sorted + "]")
                    .step();
        }

        emit.at("done")
                .say("%d account(s) collapse into %d merged account(s): %s",
                        n, merged.size(), String.join("; ", merged))
                .var("parent[]", Arrays.toString(parent)).var("rank[]", Arrays.toString(rank))
                .var("Disjoint Sets", formatSets(parent, n))
                .var("Operation", "Done: " + merged.size() + " merged account(s)")
                .var("Merged", String.join("; ", merged))
                .step();
    }

    private static int find(int x, int[] parent) {
        if (parent[x] != x) {
            parent[x] = find(parent[x], parent);
        }
        return parent[x];
    }

    /** Read-only root lookup for display, so rendering never mutates parent[]. */
    private static int rootOf(int x, int[] parent) {
        while (parent[x] != x) {
            x = parent[x];
        }
        return x;
    }

    private static String membersOf(int[] parent, int n, int root) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= n; i++) {
            if (rootOf(i, parent) == root) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(i);
            }
        }
        return sb.toString();
    }

    private static String formatSets(int[] parent, int n) {
        Map<Integer, List<Integer>> groups = new HashMap<>();
        for (int i = 1; i <= n; i++) {
            groups.computeIfAbsent(rootOf(i, parent), k -> new ArrayList<>()).add(i);
        }
        List<List<Integer>> ordered = new ArrayList<>(groups.values());
        ordered.sort(Comparator.comparingInt(g -> g.get(0)));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ordered.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("{");
            List<Integer> group = ordered.get(i);
            for (int j = 0; j < group.size(); j++) {
                if (j > 0) sb.append(", ");
                sb.append(group.get(j));
            }
            sb.append("}");
        }
        return sb.toString();
    }
}
