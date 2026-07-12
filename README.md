## Redis Stream

<img width="1537" height="613" alt="image" src="https://github.com/user-attachments/assets/13de2b0f-683a-4d4a-a4e8-27fde06c6776" />

**상황**: 녹색 그래프인 `Redis-1:6379`가 다른 노드들에 비해 `저장 공간`, `CPU 사용량`, `명령어 실행 횟수`가 많이 높은 걸 확인 할 수 있다.

**원인**: 현재 사용중인 `Redis`의 `Stream`때문이다.

- Stream Key : "simulation-events"를 키로 사용하는 스트림을 생성한다.
- Consumer Group : "entity-history-group"를 컨슈머 그룹으로 두고 "simulation-events"를 `sub`한다.

1. 이후 Spring에서 생성된 `Consumer Group`은 `sub`한 스트림에 데이터가 적재되면 읽어온다.
2. 적재된 데이터를 가공하여 `"history:entity:id"`형태로 `Redis`에 `HashSet` 형태로 해당 엔티티의 history를 기록한다.

---

## 문제점

### 너무 많은 데이터 저장
<img width="1535" height="873" alt="image" src="https://github.com/user-attachments/assets/5af65a51-26f9-40b4-b10f-0855da94dc22" />

`"simulation-events"` 스트림에 데이터가 적재되고, 매 틱, 각 앤티티마다 `CHASE, ATTACK, RUN, SPAWN`등 특정 상태일 때마다 계속해서 `stream`에 `publish`하고 있다.
이는 케이스 마다 달라지지만, 1000틱, 2000틱, 3000틱으로 올라갈수록, 앤티티들이 많아지고 그에 따라 이벤트가 발생할수록
기하급수적으로 많은 양의 데이터를 스트림에 적재중이였다.


<img width="274" height="37" alt="image" src="https://github.com/user-attachments/assets/67e549e2-7a4e-466c-a2d1-87a65918691d" />

대략 2000틱 정도 유지했을 경우, 쌓인 스트림 데이터로 `7,285,621`개의 데이터가 스트림에 적재되었다.

### 명령어 쏠림

<img width="1533" height="873" alt="image" src="https://github.com/user-attachments/assets/635a6f35-4411-4c7c-a190-0d081d709f1a" />

10분 가량 유지된 상태에서 `stream`과 관련된 명령어들 때문에 `redis-1`노드가 다른 노드들에 비해 두 배 이상의 명령어를 소화하고 있다.
