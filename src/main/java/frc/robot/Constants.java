// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import java.util.HashMap;

import com.pathplanner.lib.util.HolonomicPathFollowerConfig;
import com.pathplanner.lib.util.PIDConstants;
import com.pathplanner.lib.util.ReplanningConfig;
import com.revrobotics.CANSparkBase.IdleMode;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.units.Unit;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.ProfiledPIDCommand;

/**
 * The Constants class provides a convenient place for teams to hold robot-wide
 * numerical or boolean
 * constants. This class should not be used for any other purpose. All constants
 * should be declared
 * globally (i.e. public static). Do not put anything functional in this class.
 *
 * <p>
 * It is advised to statically import this class (or one of its inner classes)
 * wherever the
 * constants are needed, to reduce verbosity.
 */
public final class Constants {

  public static final class FieldConstants{
    public static boolean kAutoAlignStratToggle = true;
    public static double kLowSpeakerOpeningHeight = 1.98;  //meters
    public static double kHighSpeakerOpeningHeight = 2.11;  //meters
    public static double kHighSpeakerOpeningDistanceFromWall = .46; //meters

    public static Pose2d kSpeakerBackLocation = new Pose2d(-.04, 6, new Rotation2d(0.0));
    public static Pose2d kSpeakerFrontLoation = new Pose2d(kHighSpeakerOpeningDistanceFromWall, 5.525, new Rotation2d(0.0));

    public static Pose2d kRedSpeakerBackLocation = new Pose2d(16.579342, 5.525, new Rotation2d(Units.degreesToRadians(0))); // rotation might have to be flipped

    // All of this is stolen from Isaac, thanks Isaac!!! (feel free to refactor at any time)
    public static final double VISION_FIELD_MARGIN = 0.5;
    public static final double VISION_Z_MARGIN = 0.75;
    public static final double VISION_STD_XY_SCALE = 0.1; //0.01 Rate at which vision corrects for error in position Higher = takes longer
    public static final double VISION_STD_ROT_SCALE = 0.3;//0.035; Rate at which vision corrects for error in rotation/heading Higher = takes longer

    public static final double FIELD_LENGTH = 16.5417;
    public static final double FIELD_WIDTH = 8.0136;


    // corners (blue alliance origin)
    public static final Translation3d topRightSpeaker =
        new Translation3d(
        Units.inchesToMeters(18.055),
        Units.inchesToMeters(238.815 + 13), // was 238.815 15
        Units.inchesToMeters(83.091));

    public static final Translation3d redTopRightSpeaker =
        new Translation3d(
        22.062944,
        Units.inchesToMeters(238.815 + 25), // was 238.815 20
        Units.inchesToMeters(83.091));

    public static final Translation3d topLeftSpeaker =
        new Translation3d(
            Units.inchesToMeters(18.055),
            Units.inchesToMeters(197.765), // was 197.765
            Units.inchesToMeters(83.091));

    public static final Translation3d bottomRightSpeaker =
        new Translation3d(
        0.0, 
        Units.inchesToMeters(238.815), 
        Units.inchesToMeters(78.324));
    
    public static final Translation3d redBottomRightSpeaker =
        new Translation3d(
        16.55978, 
        Units.inchesToMeters(238.815), 
        Units.inchesToMeters(78.324));

    public static final Translation3d bottomLeftSpeaker =
        new Translation3d(
        0.0, 
        Units.inchesToMeters(197.765 + 13), //15
        Units.inchesToMeters(78.324)); //y: 197.765
      
    public static final Translation3d redBottomLeftSpeaker =
        new Translation3d(
        16.55978, 
        Units.inchesToMeters(197.765 + 25), //20
        Units.inchesToMeters(78.324)); //y: 197.765

    /** Center of the speaker opening (blue alliance) */
    public static final Translation3d centerSpeakerOpening =
        bottomLeftSpeaker.interpolate(topRightSpeaker, 0.5);

    public static final Translation3d redCenterSpeakerOpening = 
        redBottomLeftSpeaker.interpolate(redTopRightSpeaker, 0.5);
  

    public static boolean kVisionEnable = false;
  }

  public static final class DriveConstants {
    // Driving Parameters - Note that these are not the maximum capable speeds of
    // the robot, rather the allowed maximum speeds
    
    //Slew Constants
    public static final double kMaxSpeedMetersPerSecond =  5.74; //changed from 4.8 bc stuff wasnt straped down
    public static final double kPhysicalMaxAngularSpeedRadiansPerSecond = 2 * 2 * Math.PI;
    public static final double kMaxAngularSpeed = 2 * Math.PI; // radians per second
    public static final double kTeleDriveMaxAngularSpeedRadiansPerSecond = //
    kMaxAngularSpeed; // was 4
    public static final double kTeleDriveMaxAccelerationUnitsPerSecond = 4; //was 2


    // Chassis configuration
    public static final double kTrackWidth = Units.inchesToMeters(24.9375); //22.5 //TODO: find all of this
    // Distance between centers of right and left wheels on robot
    public static final double kWheelBase = Units.inchesToMeters(24.9375); // 26.7
    // Distance between front and back wheels on robot
    public static final double radiusMeters = Units.inchesToMeters(17.625); ///.8879 meters preconverted before
    //the distance from the center of the robot to the furthest swerve module
    public static final SwerveDriveKinematics kDriveKinematics = new SwerveDriveKinematics(
        new Translation2d(kWheelBase / 2, kTrackWidth / 2),
        new Translation2d(kWheelBase / 2, -kTrackWidth / 2),
        new Translation2d(-kWheelBase / 2, kTrackWidth / 2),
        new Translation2d(-kWheelBase / 2, -kTrackWidth / 2));


    // Angular offsets of the modules relative to the chassis in radians
    public static final double kFrontLeftChassisAngularOffset = -Math.PI / 2; //-Math.PI / 2;
    public static final double kFrontRightChassisAngularOffset = 0; // 0
    public static final double kBackLeftChassisAngularOffset = Math.PI; // Math.PI
    public static final double kBackRightChassisAngularOffset = Math.PI / 2; // Math.PI / 2

    // SPARK FLEX CAN IDs
    public static final int kFrontLeftDrivingCanId = 1;
    public static final int kRearLeftDrivingCanId = 3;
    public static final int kFrontRightDrivingCanId = 5;
    public static final int kRearRightDrivingCanId = 7;

    public static final int kFrontLeftTurningCanId = 2;
    public static final int kRearLeftTurningCanId = 4;
    public static final int kFrontRightTurningCanId = 6; //6
    public static final int kRearRightTurningCanId = 8; //8

    public static final boolean kGyroReversed = false;
    //change
    public static final double kTurningAngleP = 0;
    public static final double kTurningAngleI = 0;
    public static final double kTurningAngleD = 0;


  }

  public static final class ModuleConstants {
    // The MAXSwerve module can be configured with one of three pinion gears: 12T, 13T, or 14T.
    // This changes the drive speed of the module (a pinion gear with more teeth will result in a
    // robot that drives faster).
    public static final int kDrivingMotorPinionTeeth = 14;

    // Invert the turning encoder, since the output shaft rotates in the opposite direction of
    // the steering motor in the MAXSwerve Module.
    public static final boolean kTurningEncoderInverted = true;

    // Calculations required for driving motor conversion factors and feed forward
    public static final double kDrivingMotorFreeSpeedRps = NeoMotorConstants.kFreeSpeedRpm / 60;
    public static final double kWheelDiameterMeters = 0.0762;
    public static final double kWheelCircumferenceMeters = kWheelDiameterMeters * Math.PI;
    // 45 teeth on the wheel's bevel gear, 22 teeth on the first-stage spur gear, 15 teeth on the bevel pinion
    public static final double kDrivingMotorReduction = (45.0 * 22) / (kDrivingMotorPinionTeeth * 15);
    public static final double kDriveWheelFreeSpeedRps = (kDrivingMotorFreeSpeedRps * kWheelCircumferenceMeters)
        / kDrivingMotorReduction;

        //TODO: maybe remove this multiplier
    public static final double kDrivingEncoderPositionFactor = (kWheelDiameterMeters * Math.PI ) 
        / kDrivingMotorReduction; // meters
    public static final double kDrivingEncoderVelocityFactor = ((kWheelDiameterMeters * Math.PI)
        / kDrivingMotorReduction) / 60.0; // meters per second

    public static final double kTurningEncoderPositionFactor = (2 * Math.PI); // radians
    public static final double kTurningEncoderVelocityFactor = (2 * Math.PI) / 60.0; // radians per second

    public static final double kTurningEncoderPositionPIDMinInput = 0; // radians
    public static final double kTurningEncoderPositionPIDMaxInput = kTurningEncoderPositionFactor; // radians

    //Drivetrain P
    public static final double kDrivingP = 0.25; //was 0.8
    public static final double kDrivingI = 0;
    public static final double kDrivingD = 0;
    public static final double kDrivingFF = 1 / kDriveWheelFreeSpeedRps; 
    public static final double kDrivingMinOutput = -1;
    public static final double kDrivingMaxOutput = 1;

    public static final double kTurningP = .9; //rip spooky 0
    public static final double kTurningI = 0;
    public static final double kTurningD = 0;
    public static final double kTurningFF = -0.00; //FIXME: changed feed forward from 0
    public static final double kTurningMinOutput = -1;
    public static final double kTurningMaxOutput = 1;

    public static final IdleMode kDrivingMotorIdleMode = IdleMode.kBrake;
    public static final IdleMode kTurningMotorIdleMode = IdleMode.kBrake;

    public static final int kDrivingMotorCurrentLimit = 50; // 80 amps
    public static final int kTurningMotorCurrentLimit = 20; // amps
  }

  public static final class OIConstants {
    public static final int kDriverControllerPort = 0;
  }
  public static final class AutoConstants {

    //literally copy pasted from pathplanner docs, isaac im so disapointed in u
    //TODO: likely going to have to revisit these pid constants
    //test
    public static final HolonomicPathFollowerConfig autoBuilderPathConfig = new HolonomicPathFollowerConfig( // HolonomicPathFollowerConfig, this should likely live in your Constants class
      new PIDConstants(5.0, 0.0 ,0.2), //original p = 5, 1st attempt: p = 5, d = 0.5, 2nd attempt: p= 5, d = 0.5, 3rd attempt: p = 5, d = 3 this caused the wheels to shutter
      new PIDConstants(5, 0.0, 0), 
      4.8, //5.0, 0, 0.2
      //DriveConstants.kMaxSpeedMetersPerSecond, // Max module speed, in m/s
      DriveConstants.radiusMeters, // Drive base radius in meters. Distance from robot center to furthest module.
      new ReplanningConfig());

    public static final double kMaxSpeedMetersPerSecond = 4.5;
    public static final double kMaxAccelerationMetersPerSecondSquared = 3;
    public static final double kMaxAngularSpeedRadiansPerSecond = Math.PI;
    public static final double kMaxAngularSpeedRadiansPerSecondSquared = Math.PI;

    public static final double kPXController = 1;
    public static final double kPYController = 1;
    public static final double kPThetaController = 1;

    // Constraint for the motion profiled robot angle controller
    public static final TrapezoidProfile.Constraints kThetaControllerConstraints = new TrapezoidProfile.Constraints(
        kMaxAngularSpeedRadiansPerSecond, kMaxAngularSpeedRadiansPerSecondSquared);
  
    public static final TrapezoidProfile.Constraints kDriveControllerConstraints = new TrapezoidProfile.Constraints(
    DriveConstants.kMaxSpeedMetersPerSecond, DriveConstants.kTeleDriveMaxAccelerationUnitsPerSecond);

    public static final TrapezoidProfile.Constraints autoBalanceConstaints = new TrapezoidProfile.Constraints(
      0.1, 0.1);
  

    public static final double maxAutoSpeed = 1.4;
    public static final double maxAutoAcceleration = 2.0;

    public static final HashMap<String, Command> eventMap = new HashMap<>();

    public static final double slowIntakeSpeed = 0.3;
    public static final double fastIntakeSpeed = 1;




  }
  public static final class NeoMotorConstants {
    public static final double kFreeSpeedRpm = 6784;
  }

  public static final class PIDSwerveConstants
  {
    public static final ProfiledPIDController thetaController = new ProfiledPIDController(0.04, 0, 0, AutoConstants.kThetaControllerConstraints);
    public static final ProfiledPIDController m_XController = new ProfiledPIDController(0.04, 0, 0, AutoConstants.kDriveControllerConstraints);
    public static final ProfiledPIDController m_YController = new ProfiledPIDController(0.1, 0, 0, AutoConstants.kDriveControllerConstraints);
    public static final ProfiledPIDController m_YAutoController = new ProfiledPIDController(0.001, 0, 0, AutoConstants.kDriveControllerConstraints);

  

  }

  public static final class FeederConstants {
    public static final int kFeederCANID = 22;
    public static final double kFeederSpeed = 1.00;
    public static final double kAmpFeederSpeed = 0.70; //0.5
    public static final double kAmpFeederAutoSpeed = 1.00; //0.5
    public static final int kFeederCurrentLimit = 50;
  }
  public static final class ShooterConstants {
    public static final int kBottomShooterMotorCANID = 29;
    public static final int kTopShooterMotorCANID = 30;
    public static final double kShooterMaxSpeed = 1.00;
    public static final double kAmpShooterMaxSpeed = 0.20; //0.2

    public static final  int kRPMSetpointOffset = 100;
    
    public static final double kShooterP = .5;
    public static final double kShooterI = 0;
    public static final double kShooterD = 0;
    public static final double kShooterAmpP = .001;
    public static final double kShooterAmpI = 0;
    public static final double kShooterAmpD = 0;
    public static final int kShootingLaunchRPM = -4300; //-4300
    public static final int kFeedingLaunchRPM = -3125;

    public static final int kAngleControlCANID = 11; //TODO hey
    public static final double kAngleControlMaxSpeed = 0.07;

    public static final double kShooterAngleP = 3.70; // was 4.50
    public static final double kShooterAngleI = 0.0;
    public static final double kShooterAngleD = 0.00;
    //public static final double kAngleHardSetpoint = 0.015;
    public static final double kAngleAmpHandoffSetpoint = 0.74819; //was 0.40999

    public static final double kSubwooferAngleSetpoint = 0.634; //was 0.2958
    public static final double kFeederSetpoint = 0.6543; // was 0.3161
    public static final double kAngle4RingSetpoint = 0.650;

    public static final double kAngleFeedforward = 0.005;
  }

  public static final class IntakeConstants {
  
    public static final int kIntakeCANID = 21; 
    public static final int kIndexerCANID = 23;
    public static final int kTimeOfFlightCANID = 0; 
    public static final double kIntakeSpeed = 1.0; 
    public static final double kIndexerSpeed = 1.0;
    public static final double kIntakeSlowSpeed = 0.65; //0.45
    public static final double kIntakeAutoSpeed = 1.00; //0.45
    public static final int kIntakeCurrentLimit = 50;
    public static final int kIndexerCurrentLimit = 50;
  }

  public static final class ClimbConstants {
    public static final int kClimbMotorCanId = 14;
    public static final int kAmpMotorCanId = 15;
    public static final double kClimbMaxSpeed = 1.00;
    public static final double kAmpMaxSpeed = 0.50;
    public static final double kAmpHandoffMaxSpeed = 0.30; //0.2
    public static final double kAmpHandoffMaxAutoSpeed = 0.50;

  }
  
  public static final class PneumaticsConstants {
    public static final int kPneumaticsModuleCANID = 20; 
  
    public static final int kIntakeSolenoidSingleChannel = 10; 

    public static final int kBrakeSolenoidSingleChannel = 9;

  }
}
