package com.stage.backend.config;

import com.stage.backend.entity.CodingChallenge;
import com.stage.backend.entity.DemandeReinitialisation;
import com.stage.backend.entity.Feedback;
import com.stage.backend.entity.Notification;
import com.stage.backend.entity.QuestionFeedback;
import com.stage.backend.entity.Utilisateur;
import com.stage.backend.enums.Role;
import com.stage.backend.enums.StatutDemandeReinit;
import com.stage.backend.enums.StatutFeedback;
import com.stage.backend.enums.StatutNotification;
import com.stage.backend.enums.TypeQuestion;
import com.stage.backend.repository.CodingChallengeRepository;
import com.stage.backend.repository.DemandeReinitialisationRepository;
import com.stage.backend.repository.FeedbackRepository;
import com.stage.backend.repository.NotificationRepository;
import com.stage.backend.repository.QuestionFeedbackRepository;
import com.stage.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Profile("standalone")
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements ApplicationRunner {

    private static final int TARGET_CHALLENGES = 48;
    private static final int TARGET_CANDIDATES = 28;
    private static final int TARGET_QUESTIONS = 16;
    private static final int TARGET_FEEDBACKS = 36;
    private static final int TARGET_DEMANDES = 18;

    private static final String[][] CHALLENGE_DEFS = {
            {"Two Sum", "Find two numbers that add up to the target.", "arrays", "30"},
            {"LRU Cache", "Design a least-recently-used cache.", "design", "45"},
            {"Valid Parentheses", "Check if brackets are balanced.", "stacks", "25"},
            {"Merge Intervals", "Merge overlapping intervals.", "intervals", "40"},
            {"Binary Tree Level Order", "Return the level order traversal of a binary tree.", "trees", "50"},
            {"Word Break", "Check if a string can be segmented into dictionary words.", "dp", "70"},
            {"Course Schedule", "Detect if you can finish all courses given prerequisites.", "graphs", "55"},
            {"Find Median from Stream", "Continuously return the median of a number stream.", "heaps", "80"},
            {"Longest Substring", "Longest substring without repeating characters.", "strings", "35"},
            {"Rotate Image", "Rotate an n x n matrix by 90 degrees.", "matrices", "40"},
            {"Clone Graph", "Return a deep copy of a connected undirected graph.", "graphs", "45"},
            {"Trapping Rain Water", "Compute how much water can be trapped after raining.", "arrays", "60"},
            {"Serialize Tree", "Serialize and deserialize a binary tree.", "trees", "55"},
            {"Kth Largest", "Find the kth largest element in an unsorted array.", "heaps", "35"},
            {"Coin Change", "Fewest coins to make up an amount.", "dp", "50"},
            {"Number of Islands", "Count islands in a 2D binary grid.", "graphs", "40"},
            {"Min Window Substring", "Smallest window covering all characters of t.", "strings", "65"},
            {"Top K Frequent", "Return the k most frequent elements.", "heaps", "40"},
            {"Pacific Atlantic", "Cells that can flow to both oceans.", "graphs", "70"},
            {"Edit Distance", "Minimum operations to convert word1 to word2.", "dp", "60"},
            {"Sliding Window Max", "Maximum value in each sliding window.", "deque", "55"},
            {"Alien Dictionary", "Order of letters from sorted alien words.", "graphs", "75"},
            {"House Robber", "Max money without robbing adjacent houses.", "dp", "30"},
            {"Decode Ways", "Number of ways to decode a digit string.", "dp", "45"},
            {"Spiral Matrix", "Return matrix elements in spiral order.", "matrices", "40"},
            {"Product Except Self", "Product of array except self without division.", "arrays", "35"},
            {"Validate BST", "Check if a binary tree is a valid BST.", "trees", "40"},
            {"Word Search", "Find if word exists in a 2D board of letters.", "backtracking", "50"},
            {"Jump Game", "Can you reach the last index?", "greedy", "35"},
            {"Meeting Rooms II", "Minimum number of meeting rooms required.", "intervals", "45"},
            {"Group Anagrams", "Group strings that are anagrams.", "strings", "30"},
            {"Daily Temperatures", "Days until a warmer temperature.", "stacks", "40"},
            {"Path Sum III", "Number of paths that sum to target.", "trees", "50"},
            {"Network Delay Time", "Time for signal to reach all nodes.", "graphs", "55"},
            {"Unique Paths", "Robot unique paths in a grid.", "dp", "30"},
            {"Search Rotated Array", "Search in rotated sorted array.", "binary-search", "40"},
            {"Implement Trie", "Insert search and startsWith operations.", "design", "50"},
            {"Max Subarray", "Contiguous subarray with largest sum.", "arrays", "25"},
            {"Task Scheduler", "Least units of time for CPU tasks.", "greedy", "55"},
            {"Flatten Nested List", "Iterator for nested integer lists.", "design", "45"},
            {"Longest Palindrome", "Longest palindromic substring.", "strings", "45"},
            {"Copy List Random", "Copy list with random pointer.", "linked-list", "50"},
            {"Asteroid Collision", "Simulate asteroid collisions.", "stacks", "40"},
            {"Partition Equal Subset", "Can array be partitioned into equal sum?", "dp", "55"},
            {"Bus Routes", "Least buses to destination.", "graphs", "70"},
            {"Count Bits", "Number of 1 bits for each number 0..n.", "bit", "25"},
            {"Gas Station", "Starting gas station index for circuit.", "greedy", "45"},
            {"Min Path Sum", "Minimum path sum in a grid.", "dp", "35"},
    };

    private static final String[][] QUESTION_DEFS = {
            {"How clear was the challenge statement?", "NOTE", "true"},
            {"What was the hardest part?", "TEXTE", "true"},
            {"Would you recommend this challenge?", "CHOIX", "false"},
            {"How fair was the time limit?", "NOTE", "true"},
            {"Did you enjoy the problem?", "CHOIX", "false"},
            {"Rate the difficulty", "NOTE", "true"},
            {"Any bugs in the statement?", "TEXTE", "false"},
            {"Was the starter code helpful?", "CHOIX", "false"},
            {"How useful were the examples?", "NOTE", "true"},
            {"Suggest one improvement", "TEXTE", "false"},
            {"Did you finish on time?", "CHOIX", "true"},
            {"Rate the overall experience", "NOTE", "true"},
            {"Was the tag accurate?", "CHOIX", "false"},
            {"How stressful was the challenge?", "NOTE", "false"},
            {"Would you retry a similar topic?", "CHOIX", "false"},
            {"Free comments", "TEXTE", "false"},
    };

    private static final String[] FIRST = {
            "Alice", "Bruno", "Chloe", "David", "Emma", "Farid", "Grace", "Hugo",
            "Ines", "Jules", "Karim", "Lea", "Marie", "Noah", "Omar", "Paul",
            "Quinn", "Rita", "Samir", "Tina", "Ulysse", "Vera", "Wade", "Yara",
            "Zoe", "Amine", "Nora", "Leo"
    };
    private static final String[] LAST = {
            "Martin", "Bernard", "Dubois", "Thomas", "Robert", "Petit", "Richard",
            "Durand", "Leroy", "Moreau", "Simon", "Laurent", "Lefebvre", "Michel",
            "Garcia", "David", "Bertrand", "Roux", "Vincent", "Fournier", "Morel",
            "Girard", "Andre", "Lefevre", "Mercier", "Dupont", "Lambert", "Bonnet"
    };

    private final UtilisateurRepository utilisateurRepository;
    private final CodingChallengeRepository codingChallengeRepository;
    private final NotificationRepository notificationRepository;
    private final QuestionFeedbackRepository questionFeedbackRepository;
    private final FeedbackRepository feedbackRepository;
    private final DemandeReinitialisationRepository demandeReinitialisationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        Utilisateur demoUser = utilisateurRepository.findByEmail("demo.user@codepulse.local")
                .orElseGet(() -> ensureUser(
                        "demo.user@codepulse.local", "Demo", "User", "demo.user",
                        "Demo1234!", Role.USER, 90002L
                ));

        seedQuestions();
        List<Utilisateur> candidates = seedCandidates();
        if (!candidates.contains(demoUser)) {
            candidates.add(0, demoUser);
        }
        List<CodingChallenge> challenges = seedChallenges();
        seedNotifications(demoUser, candidates, challenges);
        seedRelanceFixtures(challenges);
        seedFeedbacks(candidates, challenges);
        seedDemandes(candidates);

        log.info(
                "Standalone demo data ready — challenges={} candidates={} questions={} feedbacks={} demandes={}",
                codingChallengeRepository.countBySupprimeFalse(),
                utilisateurRepository.countByRoleAndSupprimeFalse(Role.USER),
                questionFeedbackRepository.countBySupprimeFalse(),
                feedbackRepository.countBySupprimeFalse(),
                demandeReinitialisationRepository.count()
        );
    }

    private void seedQuestions() {
        long existing = questionFeedbackRepository.countBySupprimeFalse();
        if (existing < TARGET_QUESTIONS) {
            List<QuestionFeedback> batch = new ArrayList<>();
            for (int i = (int) existing; i < TARGET_QUESTIONS && i < QUESTION_DEFS.length; i++) {
                String[] d = QUESTION_DEFS[i];
                batch.add(question(d[0], TypeQuestion.valueOf(d[1]), Boolean.parseBoolean(d[2])));
            }
            if (!batch.isEmpty()) {
                questionFeedbackRepository.saveAll(batch);
                log.info("Demo questions seeded (+{})", batch.size());
            }
        }
        // Backfill CHOIX options on older seeded questions that had none.
        List<QuestionFeedback> missingChoix = questionFeedbackRepository.findBySupprimeFalse().stream()
                .filter(q -> q.getType() == TypeQuestion.CHOIX)
                .filter(q -> q.getChoix() == null || q.getChoix().isEmpty())
                .peek(q -> q.setChoix(defaultChoicesFor(q.getLibelle())))
                .toList();
        if (!missingChoix.isEmpty()) {
            questionFeedbackRepository.saveAll(missingChoix);
            log.info("Demo CHOIX options backfilled (+{})", missingChoix.size());
        }
    }

    private List<Utilisateur> seedCandidates() {
        List<Utilisateur> users = new ArrayList<>(utilisateurRepository.findByRole(Role.USER));
        int next = users.size();
        while (users.size() < TARGET_CANDIDATES) {
            int i = next++;
            String prenom = FIRST[i % FIRST.length];
            String nom = LAST[i % LAST.length];
            String username = (prenom + "." + nom + "." + (91000 + i)).toLowerCase();
            String email = username + "@codepulse.demo";
            Utilisateur u = ensureUser(
                    email,
                    prenom,
                    nom,
                    username,
                    "Demo1234!",
                    Role.USER,
                    92000L + i
            );
            users.add(u);
        }
        return users;
    }

    private List<CodingChallenge> seedChallenges() {
        List<CodingChallenge> all = new ArrayList<>(codingChallengeRepository.findBySupprimeFalse());
        int next = all.size();
        List<CodingChallenge> created = new ArrayList<>();
        while (all.size() < TARGET_CHALLENGES && next < CHALLENGE_DEFS.length) {
            String[] d = CHALLENGE_DEFS[next];
            long externalId = 93000L + next;
            if (codingChallengeRepository.findByExternalId(externalId).isPresent()) {
                next++;
                continue;
            }
            CodingChallenge c = challenge(
                    externalId,
                    d[0] + " #" + (next + 1),
                    d[1],
                    d[2],
                    Integer.parseInt(d[3])
            );
            c.setDateCompletion(ZonedDateTime.now().minusHours(2L + next));
            created.add(c);
            next++;
            if (created.size() >= 20) {
                all.addAll(codingChallengeRepository.saveAll(created));
                created.clear();
            }
        }
        if (!created.isEmpty()) {
            all.addAll(codingChallengeRepository.saveAll(created));
        }
        if (next > codingChallengeRepository.countBySupprimeFalse() - TARGET_CHALLENGES) {
            log.info("Demo challenges total={}", codingChallengeRepository.countBySupprimeFalse());
        }
        return codingChallengeRepository.findBySupprimeFalse();
    }

    private void seedNotifications(
            Utilisateur demoUser,
            List<Utilisateur> candidates,
            List<CodingChallenge> challenges
    ) {
        if (challenges.isEmpty()) {
            return;
        }
        int forDemo = Math.min(challenges.size(), 40);
        for (int i = 0; i < forDemo; i++) {
            StatutNotification statut = i % 5 == 0 ? StatutNotification.LUE : StatutNotification.ENVOYEE;
            ensureNotification(demoUser, challenges.get(i), statut);
        }
        int extra = Math.min(challenges.size(), 30);
        for (int i = 0; i < extra; i++) {
            Utilisateur user = candidates.get(i % candidates.size());
            if (user.getId().equals(demoUser.getId())) {
                continue;
            }
            ensureNotification(user, challenges.get(i), StatutNotification.ENVOYEE);
        }
        log.info("Demo notifications ensured for demo.user and sample candidates");
    }

    private void seedRelanceFixtures(List<CodingChallenge> challenges) {
        if (challenges.size() < 2) {
            return;
        }
        Utilisateur pending = utilisateurRepository.findByEmail("pending.setup@codepulse.demo")
                .orElseGet(() -> {
                    Utilisateur u = new Utilisateur();
                    u.setEmail("pending.setup@codepulse.demo");
                    u.setPrenom("Pending");
                    u.setNom("Setup");
                    u.setUserName("pending.setup");
                    u.setPassword(null);
                    u.setRole(Role.USER);
                    u.setCompteComplet(false);
                    u.setStatus(false);
                    u.setExternalId(93001L);
                    u.setSetupToken(UUID.randomUUID().toString());
                    u.setSetupTokenExpiresAt(ZonedDateTime.now().minusHours(2));
                    return utilisateurRepository.save(u);
                });
        Utilisateur demoUser = utilisateurRepository.findByEmail("demo.user@codepulse.local").orElse(null);
        ensureOverdueNotification(pending, challenges.get(0));
        if (demoUser != null) {
            ensureOverdueNotification(demoUser, challenges.get(challenges.size() - 1));
        }
    }

    private void ensureOverdueNotification(Utilisateur user, CodingChallenge challenge) {
        ZonedDateTime overdue = ZonedDateTime.now().minusHours(25);
        notificationRepository
                .findByUtilisateurIdAndCodingChallengeId(user.getId(), challenge.getId())
                .ifPresentOrElse(n -> {
                    if (n.getStatut() != StatutNotification.LUE && n.getNombreRelances() == 0) {
                        n.setDateEnvoi(overdue);
                        notificationRepository.save(n);
                    }
                }, () -> {
                    Notification n = new Notification();
                    n.setUtilisateur(user);
                    n.setCodingChallenge(challenge);
                    n.setDateEnvoi(overdue);
                    n.setStatut(StatutNotification.ENVOYEE);
                    notificationRepository.save(n);
                });
    }

    private void seedFeedbacks(List<Utilisateur> candidates, List<CodingChallenge> challenges) {
        long existing = feedbackRepository.countBySupprimeFalse();
        if (existing >= TARGET_FEEDBACKS || challenges.isEmpty() || candidates.isEmpty()) {
            return;
        }
        List<Feedback> batch = new ArrayList<>();
        for (int i = (int) existing; i < TARGET_FEEDBACKS && i < challenges.size(); i++) {
            CodingChallenge c = challenges.get(i);
            if (feedbackRepository.existsByCodingChallengeId(c.getId())) {
                continue;
            }
            Utilisateur user = candidates.get(i % candidates.size());
            Feedback f = new Feedback();
            f.setUtilisateur(user);
            f.setCodingChallenge(c);
            f.setNoteGlobale(2.5f + (i % 6) * 0.5f);
            f.setCommentaire("Demo feedback #" + (i + 1) + " on " + c.getTitre());
            f.setStatutFeedback(StatutFeedback.SOUMIS);
            f.setCreatedAt(ZonedDateTime.now().minusDays(i % 14).minusHours(i % 8));
            f.setChallengeTitre(c.getTitre());
            f.setChallengeTag(c.getTag());
            f.setChallengeDescription(c.getDescription());
            batch.add(f);
        }
        if (!batch.isEmpty()) {
            feedbackRepository.saveAll(batch);
            log.info("Demo feedbacks seeded (+{})", batch.size());
        }
    }

    private void seedDemandes(List<Utilisateur> candidates) {
        long existing = demandeReinitialisationRepository.count();
        if (existing >= TARGET_DEMANDES || candidates.isEmpty()) {
            return;
        }
        List<DemandeReinitialisation> batch = new ArrayList<>();
        for (int i = (int) existing; i < TARGET_DEMANDES && i < candidates.size(); i++) {
            Utilisateur u = candidates.get(i);
            DemandeReinitialisation d = new DemandeReinitialisation();
            d.setEmail(u.getEmail());
            d.setUtilisateur(u);
            d.setStatut(i % 4 == 0 ? StatutDemandeReinit.LIEN_ENVOYE : StatutDemandeReinit.EN_ATTENTE);
            d.setDateDemande(ZonedDateTime.now().minusHours(i + 1));
            batch.add(d);
        }
        if (!batch.isEmpty()) {
            demandeReinitialisationRepository.saveAll(batch);
            log.info("Demo password-reset requests seeded (+{})", batch.size());
        }
    }

    private Utilisateur ensureUser(
            String email,
            String prenom,
            String nom,
            String username,
            String rawPassword,
            Role role,
            Long externalId
    ) {
        return utilisateurRepository.findByEmail(email).orElseGet(() -> {
            Utilisateur u = new Utilisateur();
            u.setEmail(email);
            u.setPrenom(prenom);
            u.setNom(nom);
            u.setUserName(username);
            u.setPassword(passwordEncoder.encode(rawPassword));
            u.setRole(role);
            u.setCompteComplet(true);
            u.setStatus(true);
            u.setExternalId(externalId);
            return utilisateurRepository.save(u);
        });
    }

    private CodingChallenge challenge(Long externalId, String titre, String description, String tag, int duree) {
        CodingChallenge c = new CodingChallenge();
        c.setExternalId(externalId);
        c.setTitre(titre);
        c.setDescription(description);
        c.setTag(tag);
        c.setDuree(duree);
        c.setCodeUrl("https://example.com/" + tag);
        c.setParameter(false);
        c.setDateCompletion(ZonedDateTime.now().minusHours(2));
        return c;
    }

    private QuestionFeedback question(String libelle, TypeQuestion type, boolean obligatoire) {
        QuestionFeedback q = new QuestionFeedback();
        q.setLibelle(libelle);
        q.setType(type);
        q.setObligatoire(obligatoire);
        if (type == TypeQuestion.CHOIX) {
            q.setChoix(defaultChoicesFor(libelle));
        }
        return q;
    }

    private List<String> defaultChoicesFor(String libelle) {
        String key = libelle == null ? "" : libelle.toLowerCase();
        if (key.contains("recommend") || key.contains("enjoy") || key.contains("retry") || key.contains("helpful") || key.contains("accurate")) {
            return List.of("Yes", "No", "Partially");
        }
        if (key.contains("finish") || key.contains("on time")) {
            return List.of("Yes", "No", "Almost");
        }
        return List.of("Yes", "No", "Not sure");
    }

    private void ensureNotification(Utilisateur user, CodingChallenge challenge, StatutNotification statut) {
        ensureNotification(user, challenge, statut, ZonedDateTime.now().minusMinutes(30));
    }

    private void ensureNotification(
            Utilisateur user,
            CodingChallenge challenge,
            StatutNotification statut,
            ZonedDateTime dateEnvoi
    ) {
        notificationRepository
                .findByUtilisateurIdAndCodingChallengeId(user.getId(), challenge.getId())
                .orElseGet(() -> {
                    Notification n = new Notification();
                    n.setUtilisateur(user);
                    n.setCodingChallenge(challenge);
                    n.setDateEnvoi(dateEnvoi);
                    n.setStatut(statut);
                    return notificationRepository.save(n);
                });
    }
}
