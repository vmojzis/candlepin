/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.model;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents the type of consumer. See ProductFactory for some examples.
 */
@XmlRootElement(name = "consumertype")
@XmlAccessorType(XmlAccessType.PROPERTY)
@Entity
@Table(name = ConsumerType.DB_TABLE)
public class ConsumerType extends AbstractHibernateObject<ConsumerType> {

    /** Name of the table backing this object in the database */
    public static final String DB_TABLE = "cp_consumer_type";

    /**
     * Initial DB values that are part of a "basic" install ConsumerTypeEnum
     */
    public enum ConsumerTypeEnum {
        SYSTEM("system", false), PERSON("person", false), DOMAIN("domain", false),
        CANDLEPIN("candlepin", true), HYPERVISOR("hypervisor", false);

        private final String label;
        private final boolean manifest;

        ConsumerTypeEnum(String label, boolean manifest) {
            this.label = label;
            this.manifest = manifest;
        }

        /**
         * @return the label
         */
        public String getLabel() {
            return this.label;
        }

        public boolean isManifest() {
            return this.manifest;
        }

        @Override
        public String toString() {
            return getLabel();
        }
    }


    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(length = 32)
    @NotNull
    private String id;

    @Column(nullable = false, unique = true)
    @Size(max = 255)
    @NotNull
    private String label;

    @Column(nullable = false, length = 1)
    @Type(type = "yes_no")
    @NotNull
    private boolean manifest = false;

    /**
     * default ctor
     */
    public ConsumerType() {
    }

    public ConsumerType(ConsumerTypeEnum type) {
        this.label = type.getLabel();
        this.manifest = type.isManifest();
    }

    /**
     * ConsumerType constructor with label
     *
     * @param labelIn to set
     */
    public ConsumerType(String labelIn) {
        this.label = labelIn;
        for (ConsumerTypeEnum cte : ConsumerTypeEnum.values()) {
            if (cte.getLabel().equals(labelIn)) {
                this.manifest = cte.isManifest();
                break;
            }
        }
    }

    /** {@inheritDoc} */
    public String getId() {
        return id;
    }

    /**
     * @param id type id
     *
     * @return
     *  a reference to this ConsumerType instance
     */
    public ConsumerType setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * @return Returns the label.
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param labelIn The label to set.
     *
     * @return
     *  a reference to this ConsumerType instance
     */
    public ConsumerType setLabel(String labelIn) {
        label = labelIn;
        return this;
    }

    public boolean isType(ConsumerTypeEnum type) {
        return type != null && type.getLabel().equals(this.getLabel());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("ConsumerType [id: %s, label: %s, manifest: %b]",
            this.getId(), this.getLabel(), this.isManifest());
    }

    @Override
    public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (!(anObject instanceof ConsumerType)) {
            return false;
        }

        ConsumerType another = (ConsumerType) anObject;

        return label.equals(another.getLabel());
    }

    @Override
    public int hashCode() {
        return label.hashCode();
    }

    public boolean isManifest() {
        return this.manifest;
    }

    public ConsumerType setManifest(boolean manifest) {
        this.manifest = manifest;
        return this;
    }

}
