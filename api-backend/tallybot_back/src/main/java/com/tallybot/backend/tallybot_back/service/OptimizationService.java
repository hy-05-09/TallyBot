package com.tallybot.backend.tallybot_back.service;

import org.springframework.data.util.Pair;
import com.tallybot.backend.tallybot_back.debtopt.Graph;
import com.tallybot.backend.tallybot_back.domain.*;
import com.tallybot.backend.tallybot_back.repository.CalculateDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class OptimizationService {

    private final CalculateDetailRepository calculateDetailRepository;

    public void calculateAndOptimize(List<Settlement> settlementList) {
        List<CalculateDetail> lcd = calculateShare(settlementList);
        lcd = optimize(settlementList, lcd);


        calculateDetailRepository.saveAll(lcd);
    }

    private List<CalculateDetail> calculateShare(List<Settlement> sm) {
        Map<Pair<Member, Member>, Integer> m = new HashMap<>();



        for (Settlement s : sm) {
            int totalAmount = s.getAmount();
            int fixedAmount = s.getParticipants().stream().mapToInt(Participant::getConstant).sum();
            int remainingAmount = totalAmount - fixedAmount;

            double totalRatio = s.getParticipants().stream()
                    .mapToDouble(p -> p.getRatio().toDouble())
                    .sum();


            for (Participant pc : s.getParticipants()) {
                Member payer = s.getPayer(); // 돈 낸 사람 (받을 사람)
                Member payee = pc.getParticipantKey().getMember(); // 참여자 (줄 사람)

                // 돈을 줘야 하는 사람 → 받은 사람
                Pair<Member, Member> p = Pair.of(payee, payer);

                int constant = pc.getConstant();
                double ratio = pc.getRatio().toDouble();
                int ratioShare = (totalRatio == 0)
                        ? 0
                        : (int) Math.round(ratio / totalRatio * remainingAmount);


                int totalShare = constant + ratioShare;


                m.put(p, m.getOrDefault(p, 0) + totalShare);
            }
        }

        Calculate calculate = sm.get(0).getCalculate();
        List<CalculateDetail> lcd = new ArrayList<>();
        for (Map.Entry<Pair<Member, Member>, Integer> entry : m.entrySet()) {
            lcd.add(new CalculateDetail(
                    null, calculate,
                    entry.getKey().getFirst(),  // payer → 돈을 주는 사람
                    entry.getKey().getSecond(), // payee → 돈을 받는 사람
                    entry.getValue()));
        }

        return lcd;
    }



    private List<CalculateDetail> optimize(List<Settlement> sm, List<CalculateDetail> lcd) {
        Set<Member> members = new HashSet<>();
        Calculate calculate = sm.get(0).getCalculate();


        Map<Member, Integer> totalPaid = new HashMap<>();
        Map<Member, Integer> totalReceived = new HashMap<>();

        for (CalculateDetail cd : lcd) {
            totalPaid.put(cd.getPayer(), totalPaid.getOrDefault(cd.getPayer(), 0) + cd.getAmount());
            totalReceived.put(cd.getPayee(), totalReceived.getOrDefault(cd.getPayee(), 0) + cd.getAmount());
            members.add(cd.getPayer());
            members.add(cd.getPayee());
        }

        Set<Member> allMembers = new HashSet<>();
        allMembers.addAll(totalPaid.keySet());
        allMembers.addAll(totalReceived.keySet());


        // 참여 멤버 인덱스 구성
        List<Member> memberList = new ArrayList<>(members);
        Graph graph = new Graph(memberList.size());

        for (CalculateDetail cd : lcd) {
            int payerNum = memberList.indexOf(cd.getPayer());
            int payeeNum = memberList.indexOf(cd.getPayee());
            graph.addEdge(payerNum, payeeNum, cd.getAmount());
        }

        // summarize로 그래프 최적화
        Graph graph2 = Graph.summarize(graph);
        if (graph2.getEdgeCount() < graph.getEdgeCount()) {
            graph = graph2;
        }

        // 최적화된 결과로 CalculateDetail 재구성
        List<CalculateDetail> lcd2 = new ArrayList<>();
        for (int i = 0; i < graph.getVertexCount(); i++) {
            for (Integer j : graph.getAdjacencyList().get(i).keySet()) {
                int amount = graph.getAdjacencyList().get(i).get(j);
                if (amount < 0) continue;

                Member payer = memberList.get(i);
                Member payee = memberList.get(j);
                lcd2.add(new CalculateDetail(null, calculate, payer, payee, amount));
            }
        }

        return lcd2;
    }


}
