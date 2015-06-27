package playground;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity(name = "ctct")
public class Contact {
    @Id
    private String id;

    public String id() {
        return id;
    }
}
