package org.lucentrix.ingest.plugin.dummy.model.insurance;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import net.datafaker.providers.base.Address;
import net.datafaker.providers.base.Name;
import org.lucentrix.ingest.ChangeOp;
import org.lucentrix.ingest.SourceDocument;
import org.lucentrix.ingest.ContentChange;
import org.lucentrix.ingest.metadata.field.Field;
import org.lucentrix.ingest.metadata.field.FieldType;
import org.lucentrix.ingest.plugin.dummy.DocumentGenerator;

import java.time.LocalDate;
import java.time.Period;
import java.util.Random;
import java.util.UUID;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class ClientGenerator extends DocumentGenerator<InsuranceModel> {

    public ClientGenerator(Random random, int count, InsuranceModel model) {
        super(random, count, model);
    }

    @Override
    public int getLimit() {
        return settings.clientMaxCount();
    }

    @Override
    protected ContentChange generate() {
        SourceDocument.SourceDocumentBuilder<?, ?> builder = SourceDocument.builder();

        builder.field(Field.ID, new UUID(random.nextLong(), random.nextLong()).toString());
        builder.field(Field.CLASS_NAME, "Client");
        builder.field(Field.of("client_id"), count.get());

        addRandomPerson(builder);

        return ContentChange.builder().action(ChangeOp.REPLACE).document(builder.build()).build();
    }


    protected void addRandomPerson(SourceDocument.SourceDocumentBuilder<?, ?> builder) {
        Name name = faker.name();

        boolean male = random.nextBoolean();

        String firstName = male ? name.maleFirstName() : name.femaleFirstName();
        String lastName = faker.name().lastName();
        String fullName = faker.name().fullName();
        String sex = faker.demographic().sex();
        LocalDate dob = faker.date().birthdayLocalDate(18, 60);


        // Calculate age
        int age = Period.between(dob, LocalDate.now()).getYears();

        Address address = faker.address();
        String streetAddress = address.streetAddress();
        String city = address.city();
        String state = address.state();
        String zip = address.zipCode();
        String fullAddress = String.format("%s, %s, %s %s", streetAddress, city, state, zip);

        String phone = faker.phoneNumber().cellPhone();
        String ssn = faker.idNumber().ssnValid(); // or .ssn()


        builder.field(Field.of("firstname"), firstName);
        builder.field(Field.of("lastname"), lastName);
        builder.field(Field.of("fullname"), fullName);
        builder.field(Field.of("address"), fullAddress);

        builder.field(Field.of("sex"), sex);
        builder.field(Field.of("age"), age);
        builder.field(Field.of("dateofbirth", FieldType.DATETIME), dob);
        builder.field(Field.of("phone"), phone);
        builder.field(Field.of("ssn"), ssn);

    }
}
