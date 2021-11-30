package nextstep.subway.line.domain;

import nextstep.subway.station.domain.Station;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import java.util.*;
import java.util.function.Predicate;

@Embeddable
public class Sections {

    @OneToMany(mappedBy = "line", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Section> sections = new ArrayList<>();

    protected Sections() {
    }

    public Sections(List<Section> sections) {
        this.sections = sections;
    }

    public static Sections of(Section... sections) {
        Sections newSections = new Sections();
        for (Section section : sections) {
            newSections.add(section);
        }
        return newSections;
    }

    public static Sections empty() {
        return new Sections();
    }

    public void add(Section section) {
        validateDuplicatedStation(section);
        validateNotFoundStation(section);

        this.sections
                .stream()
                .filter(s -> s.hasEqualUpStation(section))
                .findFirst()
                .ifPresent(s -> s.updateUpSection(section));

        this.sections
                .stream()
                .filter(s -> s.hasEqualDownStation(section))
                .findFirst()
                .ifPresent(s -> s.updateDownSection(section));

        this.sections.add(section);
    }

    public List<Station> orderedStations() {
        Section firstSection = findFirstSection();
        return createStations(firstSection);
    }

    public void remove(Station station) {
        validateOnlyOneSection();

        Section firstSection = findFirstSection();
        if (firstSection.isSameUpStation(station)) {
            this.sections.remove(firstSection);
            return;
        }

        Section lastSection = findLastSection();
        if (lastSection.isSameDownStation(station)) {
            this.sections.remove(lastSection);
            return;
        }

        removeMiddleSection(station);
    }

    private void validateOnlyOneSection() {
        if (this.sections.size() == 1) {
            throw new IllegalArgumentException("현재 구간이 1개라 제거할 수 없습니다.");
        }
    }

    private void removeMiddleSection(Station station) {
        Section upSection = findSectionIsDownStation(station);
        Section downSection = findSectionIsUpStation(station);
        remove(upSection, downSection);
    }

    private void remove(Section upSection, Section downSection) {
        upSection.updateForRemove(downSection);
        this.sections.remove(downSection);
    }

    private Section findSectionIsUpStation(Station station) {
        return this.sections
                .stream()
                .filter(s -> s.hasEqualUpStation(station))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("현재 노선에 존재하지 않는 지하철 역입니다. (입력값: " + station.getName() + ")"));
    }

    private Section findSectionIsDownStation(Station station) {
        return this.sections
                .stream()
                .filter(s -> s.hasEqualDownStation(station))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("현재 노선에 존재하지 않는 지하철 역입니다. (입력값: " + station.getName() + ")"));
    }

    private Section findFirstSection() {
        List<Station> upStations = new ArrayList<>();
        List<Station> downStations = new ArrayList<>();
        addUpAndDownStations(upStations, downStations);
        upStations.removeAll(downStations);
        Station firstStation = upStations.get(0);

        return findSection(s -> s.getUpStation() == firstStation, "첫번째");
    }

    private Section findLastSection() {
        List<Station> upStations = new ArrayList<>();
        List<Station> downStations = new ArrayList<>();
        addUpAndDownStations(upStations, downStations);
        downStations.removeAll(upStations);
        Station lastStation = downStations.get(0);

        return findSection(s -> s.getDownStation() == lastStation, "마지막");
    }

    private Section findSection(Predicate<Section> sectionPredicate, String errorMessageKeyword) {
        return this.sections
                .stream()
                .filter(sectionPredicate)
                .findFirst().orElseThrow(() -> new IllegalArgumentException("노선의 " + errorMessageKeyword + "구간이 존재하지 않습니다."));
    }

    private void addUpAndDownStations(List<Station> upStations, List<Station> downStations) {
        for (Section section : this.sections) {
            upStations.add(section.getUpStation());
            downStations.add(section.getDownStation());
        }
    }

    private List<Station> createStations(Section section) {
        List<Station> stations = new ArrayList<>();
        stations.add(section.getUpStation());
        stations.add(section.getDownStation());

        Optional<Section> optionalSection = findNextSection(section);
        while (optionalSection.isPresent()) {
            Section nextSection = optionalSection.get();
            stations.add(nextSection.getDownStation());
            optionalSection = findNextSection(nextSection);
        }
        return stations;
    }

    private Optional<Section> findNextSection(Section section) {
        return this.sections
                .stream()
                .filter(s -> s.getUpStation() == section.getDownStation())
                .findFirst();
    }

    private void validateDuplicatedStation(Section section) {
        Optional<Section> optionalSection = this.sections
                .stream()
                .filter(s -> s.getUpStation() == section.getUpStation())
                .findFirst()
                .filter(s -> s.getDownStation() == section.getDownStation());

        if (optionalSection.isPresent()) {
            throw new IllegalArgumentException("이미 등록되어 있는 노선입니다.");
        }
    }

    private void validateNotFoundStation(Section section) {
        if (this.sections.isEmpty()) {
            return;
        }

        this.sections
                .stream()
                .filter(s -> s.isContainsStation(section))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("상행역과 하행역 둘 중 하나도 포함되어있지 않습니다."));
    }

    public List<Section> getSections() {
        return Collections.unmodifiableList(sections);
    }
}
