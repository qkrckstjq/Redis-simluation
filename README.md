# Redis Simulation
**Redis**를 메인 데이터 저장소로 사용하여 수만 개의 엔티티가 실시간으로 상호작용하는 시뮬레이션을 구현하고

**Redis Cluster** 환경에서 성능과 운영성을 검증하는 프로젝트입니다.

- 100ms Tick 기반 실시간 시뮬레이션
- Redis Cluster(3 Master)
- 최대 약 64,000개의 엔티티 처리
- Redis GEO · Streams · Pipeline 기반 구현

<img width="1000" height="563" alt="Redis Simulation - Chrome 2026-07-16 19-23-41 (1)" src="https://github.com/user-attachments/assets/f0b9fc3f-6fbf-4ead-8560-53e1e03eba68" />


# 프로젝트 소개
엔티티들은 알고리즘을 기반으로 2차원 좌표 평면에서 행동합니다.

모든 행동은 100ms Tick 단위로 처리되며, 이동·사냥·도망·번식과 같은 상태 변화가 발생합니다.

엔티티의 상태는 모두 Redis에 저장되며, Tick마다 저장이 필요한 정보를 Redis에 반영합니다.

이러한 과정에서 Redis GEO, Streams, Cluster, Consumer Group 등의 기능을 활용하여 실시간 상태 관리와 이벤트 처리를 수행하고, Redis의 부하 및 장애 상황을 모니터링합니다.


# 프로젝트 핵심 기능

| 기능 | 설명 |
|------|------|
| **Tick 기반 실시간 시뮬레이션** | 100ms Tick마다 수천~수만 개의 엔티티 상태를 갱신하며 실시간 시뮬레이션을 수행합니다. |
| **알고리즘 기반 상태 판단** | 늑대와 양의 이동, 추적, 공격, 도망, 군집(Flocking), 번식 등의 행동을 상태(State) 기반으로 처리합니다. |
| **Redis GEO 기반 주변 엔티티 탐색** | 반경 내 엔티티를 탐색하여 AI 의사결정과 충돌 처리를 수행하며, Cell 기반 공간 분할을 통해 탐색 성능을 최적화했습니다. |
| **Redis Pipeline 기반 대량 데이터 처리** | Tick마다 발생하는 대량의 읽기/쓰기 요청을 Pipeline으로 처리하여 Redis와 Spring 간 네트워크 오버헤드를 줄였습니다. |
| **Redis Cluster 기반 데이터 분산** | Hash Tag와 Cell 단위 Key 설계를 통해 데이터를 분산 저장하고 Cluster 환경에서 시뮬레이션을 수행합니다. |
| **Redis Streams 기반 이벤트 처리** | 사냥, 번식, 죽음 등의 이벤트를 Stream에 기록하여 비동기 이벤트 처리와 메트릭 수집에 활용합니다. |
| **Consumer 장애 복구 및 DLQ 관리** | Pending Message Recovery, Claim 처리, Dead Letter Queue를 구현하여 Consumer 장애 상황에서도 이벤트 유실을 최소화했습니다. |
| **Prometheus · Grafana 기반 모니터링** | Spring과 Redis의 CPU, Memory, Entity 수, Stream 처리량 등을 실시간으로 모니터링하고 부하 테스트 결과를 시각화합니다. |

---

# 기술 스택
| 카테고리 | 기술 |
| - | - |
| BackEnd | Java, Spring, WebSocket |
| FrontEnd | HTML, CSS, JavaScript |
| DataBase | Redis Cluster |
| Monitoring | Prometheus, Grafana |
| Infra | Docker, Docker Compose |

---

# Redis 선택 이유

Redis는 다양한 자료구조(Hash, Set, List)와 함께 **GEO, Streams, Cluster** 기능을 제공하여 실시간 위치 탐색, 이벤트 처리, 데이터 분산 저장을 하나의 저장소에서 구현할 수 있습니다.

이 프로젝트에서는 이러한 기능을 활용하여 엔티티 상태 관리, 주변 탐색, 이벤트 처리, 분산 저장을 하나의 데이터 저장소에서 처리하기 위해 Redis를 메인 데이터 저장소로 선택했습니다.

---

# 시뮬레이션 설명

## 엔티티 종류

시뮬레이션은 `SHEEP`와 `WOLF` 두 종류의 엔티티로 구성됩니다.

### 🟩 SHEEP

- 비공격 엔티티
- 주변 Sheep과 군집(Flocking) 행동 수행
- 번식 조건을 만족하면 반경 `1.5` 이내의 Sheep과 번식
- Wolf를 탐지하면 도망(Run) 상태로 전환

### 🟥 WOLF

- 공격 엔티티
- 반경 `10` 이내의 Sheep을 탐색
- Sheep을 추적(Chase) 후 공격(Attack)
- 사냥 성공 시 체력과 스테미나 회복
- 번식 조건을 만족하면 다른 Wolf와 번식

---

## 공통 로직

### SEARCH

모든 엔티티는 `Redis GEO Search`를 이용하여 주변 엔티티를 탐색하고 다음 행동을 결정합니다.

### AGE

모든 엔티티는 Tick마다 나이가 증가하며, Age가 `1000`을 초과하면 사망합니다.

### BREED

번식 가능한 엔티티는 가까운 동일 타입의 엔티티를 우선적으로 따라가며 번식을 시도합니다.

---

## 시뮬레이션 목표

군집을 이루며 번식하는 Sheep과 이를 사냥하는 Wolf의 상호작용을 통해 개체 수 변화와 이벤트 발생을 관찰합니다.

모든 상태 변화는 Redis에 저장되며, Redis Cluster 환경에서 발생하는 부하와 이벤트 처리 과정을 분석하는 것이 프로젝트의 핵심 목표입니다.

---

# 작동 흐름

시뮬레이션은 100ms마다 아래 순서로 수행됩니다.
| 단계 | 작업 | 설명 |
|------|------|------|
| **1** | Read Entities | EntityManager(In-Memory)에서 엔티티 정보를 조회합니다. |
| **2** | Skip GeoSearch | 주변 탐색이 필요 없는 엔티티를 필터링합니다. |
| **3** | GeoSearch | Redis GEO를 이용하여 주변 엔티티를 탐색합니다. |
| **4** | Decide Next Move | 주변 엔티티를 기반으로 다음 행동(State)을 결정합니다. |
| **5** | Execute Behavior | 이동, 공격, 도망, 번식 등의 행동을 수행하고 충돌을 처리합니다. |
| 아래부터 비동기 | - | - |
| **-** | Update Entity State | 변경된 엔티티 정보를 Redis에 반영합니다. |
| **-** | Publish Stream Events | 발생한 이벤트를 Redis Streams에 기록합니다. |
| **-** | Create Spawn Entities | 새롭게 생성된 엔티티를 Redis에 저장합니다. |
| **-** | Send Snapshot | 현재 Tick의 엔티티 정보를 WebSocket을 통해 클라이언트에 전송합니다. |
---

# 주요 기능

## Redis GEO를 활용한 주변 엔티티 탐색

모든 엔티티는 상황에 따라 매 Tick마다 주변 엔티티를 탐색하여 다음 행동을 결정합니다.

Redis GEO Search를 사용하여 반경 내 엔티티를 조회하며, 탐색 범위가 여러 Cell에 걸치는 경우 인접 Cell까지 함께 조회하여 정확한 탐색 결과를 제공합니다.

> 2차원 좌표를 Redis GEO에서 사용할 수 있도록 실제 위도·경도 좌표계로 스케일링하여 저장했습니다. 이를 통해 Redis GEO의 반경 탐색 기능을 그대로 활용하면서 2차원 시뮬레이션 환경에서 주변 엔티티를 효율적으로 정해진 좌표안에서는 큰 오차범위 없이 탐색했습니다.

<img width="136" height="125" alt="image" src="https://github.com/user-attachments/assets/652a97f5-b7d3-4f1d-a395-026acd137ab6" />
(🟥 WOLF 기준 반경10`(노란색 원)` 이내의 노란색으로 하이라이트 처리된 다른 앤티티들)

### 적용 기술

- Redis GEO
- Radius Search
- Cell 기반 공간 분할
- Multi Cell Search

---

## 알고리즘 기반 상태 판단

엔티티는 주변 환경에 따라 상태(State)를 변경하며 행동합니다.

- MOVE
- IDLE
- CHASE
- ATTACK
- RUN
- FLOCK
- SPAWN
- REST

늑대와 양은 서로 다른 상태 전이 로직을 가지며, 주변 엔티티의 종류와 거리, 체력, 스테미나, 나이 등을 고려하여 다음 행동을 결정합니다.

---

## Redis Pipeline을 이용한 대량 요청 최적화

매 Tick마다 수천~수만 건의 Redis Read/Write 요청이 발생합니다.

이를 일반 요청으로 처리할 경우 네트워크 RTT가 병목이 되므로, Pipeline을 적용하여 요청을 한 번에 전송하도록 구현했습니다.

### 적용 효과

- Redis 네트워크 RTT 감소
- Tick 처리 시간 단축
- 대량 엔티티 처리 성능 향상

---

## Redis Cluster 기반 데이터 분산

Redis Cluster 환경에서 데이터를 균등하게 분산하기 위해 Cell 단위 Key와 Hash Tag를 적용했습니다.

이를 통해 Cross Slot 문제를 방지하면서 주변 탐색과 데이터 업데이트를 수행하도록 설계했습니다.

<img width="202" height="116" alt="image" src="https://github.com/user-attachments/assets/78352120-f193-4674-8ede-dd28a3449977" />


### 적용 기술

- Redis Cluster
- Hash Tag
- Cell Partitioning

---

## Redis Streams 기반 이벤트 처리

상태 변화 중 의미 있는 이벤트를 Redis Streams에 기록합니다.

기록된 이벤트는 Consumer Group이 비동기로 처리하며, 메트릭 수집과 이벤트 분석에 활용됩니다.

<img width="327" height="190" alt="image" src="https://github.com/user-attachments/assets/a1910f9f-1e67-4adc-8485-54baca5d8494" />


### 기록 이벤트

- SPAWN
- HUNT
- DEAD

---

## Consumer 장애 복구

Consumer 장애 발생 시 Pending Entry List(PEL)에 남아 있는 메시지를 Recovery하여 다시 처리합니다.

일정 횟수 이상 재처리에 실패한 이벤트는 Dead Letter Queue(DLQ)로 이동하여 이벤트 유실을 방지했습니다.

<img width="1510" height="307" alt="image" src="https://github.com/user-attachments/assets/e54e1a03-d744-4213-8c5e-0045d3e90ebb" />

`Stream에 적재된 이벤트들과 Pending된 데이터 그라파나에서 조회가능`


### 적용 기술

- Consumer Group
- Pending Recovery
- Claim
- Dead Letter Queue

---

## WebSocket 기반 실시간 시각화

매 Tick마다 엔티티 Snapshot을 WebSocket으로 전송하여 프론트엔드에서 실시간으로 렌더링합니다.

또한 선택한 엔티티의 상태, 탐색 범위, 행동 이력 등을 함께 확인할 수 있도록 구현했습니다.

<img width="1092" height="632" alt="image" src="https://github.com/user-attachments/assets/0de93e98-ddd7-4f4f-95b4-8aeaa20a63cd" />



### 제공 기능

- 실시간 Entity 렌더링
- Entity 검색
- Range 표시
- History 조회
- HUD(FPS, Entity Count)

---

## Prometheus · Grafana 모니터링

Spring과 Redis에서 발생하는 메트릭을 Prometheus로 수집하고 Grafana를 통해 시각화했습니다.

이를 통해 엔티티 증가에 따른 CPU, Memory, Tick 처리 시간, Stream 처리량 등을 분석할 수 있도록 구성했습니다.

---

# 개선 사항

## 🔧 Redis Pipeline 적용

### 기존의 방식

엔티티 정보를 조회하거나 저장할 때 각 명령을 개별적으로 Redis에 요청하였다.

```text
HGETALL entity:1
HGETALL entity:2
HGETALL entity:3
...

HMSET entity:1
HMSET entity:2
HMSET entity:3
...
```

엔티티 수만큼 Redis와 네트워크 왕복(RTT)이 발생하였다.

### 문제점

- **Tick마다 수천 개의 Redis 명령이 개별적으로 전송되면서 네트워크 왕복(RTT)이 병목이 되었다.**

- 엔티티 수가 증가할수록 Redis 자체의 처리 시간보다 네트워크 왕복 비용의 영향이 커졌고, Tick 처리 시간이 증가하였다.

---

### 개선한 방식

Redis에서 제공하는 **Pipeline** 기능을 적용하여 여러 명령을 하나의 요청으로 묶어 처리하도록 변경하였다.

```java
public List<Object> responsePipeLine(Consumer<RedisConnection> consumer) {
    return redisTemplate.executePipelined(
            (RedisCallback<Object>) connection -> {
                consumer.accept(connection);
                return null;
            });
}
```

기존처럼 명령을 하나씩 요청하는 대신

여러 명령을 하나의 네트워크 요청으로 전송하여 Redis와의 왕복 횟수를 크게 줄였다.

### 개선된 성능

- Redis와의 네트워크 왕복(RTT) 감소
- Tick당 Read/Write 처리 시간 단축
- 엔티티 수가 증가할수록 성능 향상 효과 증가

---

## 🔧 앤티티들의 충돌 판정 개선

### 기존의 방식

다음 이동할 좌표값에 대해서
`Map<Poisiton, Long>`을 사용하여 중복될 다음 좌표들을 거르고,
다음 이동할 좌표 기준 geoSearch 반경 0.2m탐색을 통해 본인 제외, 움직이기 전 앤티티들이 반경 0.2m이내에 존재하는지에 따라 충돌 여부를 계산

### 문제점
- Collision 계산을 위해 모든 엔티티에 대해 추가적인 GEOSEARCH를 수행해야 했다.
- 엔티티 수가 증가할수록 Collision 단계에서 Redis 요청 수가 급격히 증가하였고, 전체 Tick 시간에서도 상당한 병목을 차지하게 되었다.**

---

### 개선한 방식

- Tick 시작 시 이미 HGETALL을 통해 메모리에 적재한 엔티티 목록을 활용하여 Position 기반 좌표 맵을 생성하였다.
- 다음 움직일 좌표에 대해서 기존에 생성해놓은 좌표와 비교하여 해당 좌표에 앤티티가 존재하는지 계산한뒤 움직인다.
- 이후 이동 요청마다 Position 맵만 조회하여 충돌 여부를 O(1)에 가깝게 판정하도록 변경하였다.

### 개선된 성능
| Entity Count | GEOSEARCH Collision Search | In-Memory Move With Collision | Improvement |
|-------------|---------------------------:|------------------------------:|------------:|
| 1,000 | 12.6 ms | 0.0 ms | -12.6 ms |
| 5,000 | 39.3 ms | 1.3 ms | -38.0 ms |
| 10,000 | 66.6 ms | 3.4 ms | -63.2 ms |

---

## 🔧 Redis Cluster 기반 샤딩 구조 적용

### 기존의 방식

모든 엔티티의 위치 정보를 하나의 Geo Set에서 관리하였다.

```text
geo:entities
  entity:1
  entity:2
  entity:3
 ...
```

모든 Entity Key 역시 동일한 Key 공간에서 관리하였다.

### 문제점

**단일 Redis 인스턴스에 모든 데이터가 집중되어 확장성이 제한되었다.**

또한 Cluster 환경에서는 Geo Key와 Entity Key가 서로 다른 Slot에 저장될 수 있어, 관련 데이터를 함께 처리하기 어려웠다.

---

### 개선한 방식

월드를 Cell 단위로 분할하고 Cell마다 별도의 Geo Set을 관리하도록 변경하였다.

```text
geo:0:0
geo:0:1
geo:1:0
...
```

또한 Entity Key에 Hash Tag를 적용하여 같은 Cell의 Entity와 Geo 데이터가 동일한 Hash Slot에 저장되도록 설계하였다.

```text
{geo:0:0}:entity:1
{geo:0:0}:entity:2
{geo:1:0}:entity:3
```

이를 통해 Redis Cluster 환경에서도 관련 데이터를 동일한 Slot에서 처리할 수 있도록 개선하였다.

---

### 개선 효과

- Redis Cluster 환경에서 데이터를 노드별로 분산 저장할 수 있도록 구조 개선
- Hash Tag를 적용하여 동일 Cell의 데이터를 같은 Hash Slot에 배치
- Cross Slot 문제를 방지하고 Cluster 환경에 적합한 Key 구조 설계
- Cell 단위 데이터 관리로 향후 노드 확장 및 Resharding에 대응 가능한 구조 확보

<img width="1511" height="611" alt="image" src="https://github.com/user-attachments/assets/1d59d833-a5cb-4124-bae5-8ec6685bb37c" />

| Metric | Single Redis | Redis Cluster | Improvement |
|--------|-------------:|--------------:|------------:|
| Tick Time (10,000 Entities) | 74 ms | 50 ms | ** 32% 감소** |
| Main Thread CPU | 0.29 | 0.13 | ** 55% 감소** |
| Process CPU | 0.28 | 0.13 | ** 54% 감소** |
| Commands / Node | 21,000 | 8,500 | ** 60% 감소** |
| Memory / Node | - | - | **노드별 메모리 분산** |

---

## 🔧 WebSocket 및 Redis 작업 비동기 처리

### 기존의 방식

Tick마다 엔티티의 상태를 계산한 후 Redis 업데이트, Stream 발행, WebSocket 전송을 모두 순차적으로 수행하였다.

```text
1. Tick


2. AI 계산


3. Redis Update


4. Redis Stream Publish


5. WebSocket Send

다음 Tick
```

### 문제점

**Redis 업데이트와 WebSocket 전송이 완료될 때까지 Tick 루프가 대기하였다.**

특히 엔티티 수가 증가할수록 I/O 작업의 수행 시간이 Tick 처리 시간에 그대로 포함되어 전체 처리량이 감소하였다.

---

### 개선한 방식

Redis Update, Redis Stream Publish, WebSocket 전송을 `CompletableFuture`와 별도의 `ThreadPoolTaskExecutor`를 이용하여 비동기로 수행하도록 변경하였다.

```text
Tick

↓

AI 계산

동시 시작 Redis Update (Async), Redis Stream Publish (Async), WebSocket Send (Async)

↓

다음 Tick
```

각 작업은 독립적인 Executor에서 수행되도록 분리하여 Tick 루프가 I/O 작업 완료를 기다리지 않도록 개선하였다.

### 개선 효과

- Tick 루프에서 I/O 작업 대기 시간 제거
- Redis Update, Stream Publish, WebSocket 전송을 병렬 수행
- Tick 처리 시간이 계산 중심으로 변경되어 처리량 향상
- 엔티티 수 증가 시 I/O 병목 완화

<img width="1516" height="335" alt="image" src="https://github.com/user-attachments/assets/4f8440da-6fe4-4bc2-8cf7-461eb2c67d1a" />

<img width="1513" height="308" alt="image" src="https://github.com/user-attachments/assets/d91e5b33-5b39-411b-b4f8-5b6116259c98" />

| Metric | Sync | Async | Improvement |
|--------|-----:|------:|------------:|
| Tick Time (10,000 Entities) |  570 ms |  330 ms | ** 42% 감소 (-240 ms)** |
| Redis Process CPU (Peak) |  0.26 |  0.42 | ** 62% 증가** |
| Redis Commands/sec (Peak) |  37,000 |  68,000 | ** 84% 증가** |

> 아무래도 비동기로 처리함으로써 TPS 자체는 높아졌지만 그만큼 Redis가 처리해야 하는 명령어의 밀도가 높아졌다.

---

## 🔧 Redis 중심 처리 → In-Memory 기반 아키텍처 전환

### 기존의 방식

매 Tick마다 Redis에서 엔티티 정보를 조회하고, AI 계산 후 다시 Redis에 저장하는 구조였다.

```text
Tick

↓

Redis Read (HGETALL)

↓

AI 계산

↓

Redis Update

↓

다음 Tick
```

엔티티의 모든 상태를 Redis에서 직접 관리하였기 때문에 Tick마다 대량의 Redis Read/Write가 발생하였다.

### 문제점

**Redis가 엔티티 저장소이면서 동시에 계산 대상이 되어 Tick마다 많은 I/O가 발생하였다.**

엔티티 수가 증가할수록 Redis 조회 및 저장 비용이 증가하여 Tick 처리 시간이 Redis 성능에 크게 의존하였다.

또한 이미 Tick에서 읽어온 데이터를 이후 단계에서도 다시 Redis를 조회하는 등 불필요한 Redis 접근이 반복되었다.

---

### 개선한 방식

애플리케이션 시작 시 Redis의 모든 엔티티를 메모리(EntityManager)로 적재하고, 이후 Tick은 메모리에서만 수행하도록 구조를 변경하였다.

```text
Application Start

↓

Redis Read

↓

EntityManager (In-Memory)

↓

Tick

↓

AI 계산

↓

주기적으로 Redis 동기화
```

EntityManager가 엔티티의 상태를 관리하도록 변경하여 대부분의 계산을 메모리에서 수행하고, Redis는 상태 저장 및 다른 시스템과의 데이터 공유를 위한 저장소 역할만 수행하도록 개선하였다.

### 개선 효과

- Tick 수행 중 Redis Read를 제거하여 Redis I/O 감소
- 대부분의 AI 계산을 메모리에서 수행하여 Tick 처리 시간 단축
- Redis를 계산 대상이 아닌 동기화 저장소로 역할 분리
- 이후 충돌 판정, AI 탐색 등 추가적인 인메모리 최적화를 적용할 수 있는 기반 마련

<img width="1517" height="337" alt="image" src="https://github.com/user-attachments/assets/1ef0c87c-90fc-49af-a789-1f7dfd9ca4f1" />
<img width="1512" height="612" alt="image" src="https://github.com/user-attachments/assets/43f7c190-fe54-415e-8a31-0d30232ecbd4" />

| Metric | Redis 중심 처리 | In-Memory 아키텍처 | Improvement |
|--------|----------------:|-------------------:|------------:|
| Tick Time (10,000 Entities) |  340 ms |  70 ms | ** 79% 감소 (-270 ms)** |
| Redis Memory Used |  3.2 MB |  0.6 MB | ** 81% 감소** |
| Redis Main Thread CPU (Peak) |  0.41 |  0.14 | ** 66% 감소** |
| Redis Process CPU (Peak) |  0.42 |  0.13 | ** 69% 감소** |
| Redis Commands/sec (Peak) | 70,000 |  10,000 | ** 86% 감소** |

---

## 🔧 Redis Streams Pending Recovery / DLQ 구현

### 기존의 방식

Consumer가 메시지를 처리하는 도중 장애가 발생하면 해당 메시지는 Pending Entries List(PEL)에 남게 된다.

```text
Producer

↓

Redis Stream

↓

Consumer (Processing)

↓

Consumer 장애

↓

Pending Entries List (PEL)
```

Consumer가 다시 실행되더라도 Pending 메시지를 처리하는 로직이 없어 이벤트가 계속 PEL에 남아있었다.

### 문제점

**Consumer 장애 시 Pending 메시지가 재처리되지 않아 이벤트가 유실될 가능성이 존재하였다.**

또한 동일한 메시지가 반복적으로 처리에 실패하는 경우에도 별도의 처리 전략이 없어 운영 중 장애 대응이 어려웠다.

---

### 개선한 방식

일정 주기마다 Pending Entries List를 조회하여 장시간 처리되지 않은 메시지를 Recovery하도록 구현하였다.

```text
Pending Messages

↓

XCLAIM

↓

Retry

├── Success → ACK

└── Fail

      ↓

Retry Count 확인

      ↓

DLQ 저장
```

- 일정 시간 이상 Pending 상태인 메시지를 조회
- `XCLAIM`을 이용하여 현재 Consumer가 소유권을 획득
- 재처리에 성공하면 ACK 수행
- 일정 횟수 이상 재처리에 실패한 메시지는 Dead Letter Queue(DLQ)로 이동

### 개선 효과

- Consumer 장애 발생 시 Pending 메시지 자동 복구
- 이벤트 유실 가능성 감소
- 반복적으로 실패하는 메시지를 DLQ로 분리하여 운영 안정성 향상
- 장애 발생 후에도 Consumer Group이 정상적으로 메시지를 이어서 처리할 수 있도록 개선

<img width="1548" height="825" alt="image" src="https://github.com/user-attachments/assets/8cef32a4-4e90-462a-aa3c-7665d9b0978b" />
> Prometheus + Grafana를 통해 스트림 메세지 길이 및 오류, Pending된 데이터 확인

---

## 최종 결과

초기에는 Redis를 중심으로 모든 계산을 수행하는 구조였지만, 

단계적인 최적화를 통해 병목 구조를 완화하고, 새로운 구조 도입 및 개선으로 성능을 최적화 시키고

Redis Cluster, Consumer Recovery등을 통해 안정성을 높였다.

```text
Redis Pipeline
        │
        ▼
In-Memory Collision
        │
        ▼
Redis Cluster
        │
        ▼
Async Processing
        │
        ▼
In-Memory Architecture
        │
        ▼
Pending Recovery / DLQ
```

<img width="1547" height="638" alt="image" src="https://github.com/user-attachments/assets/e96c6a5b-997c-43e7-9132-9a4c2361ac99" />

<img width="1550" height="590" alt="image" src="https://github.com/user-attachments/assets/e7eba31e-71ba-4c1a-83e1-c35667ec67e0" />

> 100ms Tick 환경에서 1,000개의 엔티티부터 시작하여 최대 약 **64,000개의 엔티티**까지 증가시키며 측정하였다.

| Metric | Result |
|--------|-------:|
| World Size | 256 × 256 |
| Initial Entity Count | 1,000 |
| Maximum Entity Count | 약 64,000 |
| Tick Time (64K Entities) | 약 **670 ms** |
| Redis Main Thread CPU (Peak) | 약 **0.68** |
| Redis Process CPU (Peak) | 약 **0.66** |
| Redis Commands/sec (Peak) | 약 **20,000 / Node** |
| Redis Stream Pending | **0** |
| Stream Error Count | **0** |

### 결과

- 최대 약 **64,000개의 엔티티**까지 Tick을 안정적으로 수행
- Redis Cluster 환경에서 노드별 Commands를 분산 처리
- Pending Recovery를 통해 Pending Count를 지속적으로 **0**으로 유지
- DLQ 및 Stream Error 없이 Consumer를 안정적으로 운영

---

Redis GeoSearch VS InMemory FindNearEntities

<img width="1509" height="700" alt="image" src="https://github.com/user-attachments/assets/a1e7aa39-deaf-4519-8e0a-954a6f2436b4" />

`좌측 : Redis GeoSearch`
`우측 : InMemory NearSearch`

Redis의 GEOSEARCH를 InMemory 탐색으로 대체하는 실험을 진행하였다. 

초기에는 네트워크 비용이 없는 InMemory 방식이 더 빠른 성능을 보였지만, 엔티티 수가 증가할수록 성능 차이는 거의 사라졌다.

또한 Redis GEOSEARCH는 거리순 탐색(ASC)과 같은 공간 검색 기능을 이미 최적화하여 제공하는 반면, InMemory 방식은 동일한 결과를 얻기 위해 별도의 거리 계산과 정렬 로직을 직접 구현해야 했다.

성능상 이점이 크지 않았고, 공간 탐색의 책임은 Redis에, AI 계산은 Spring에 분리하는 구조가 역할 분리와 유지보수 측면에서도 더 적합하다고 판단하여 Redis GEOSEARCH 방식을 최종 선택하였다.
